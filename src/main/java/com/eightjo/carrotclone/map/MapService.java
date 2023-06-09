package com.eightjo.carrotclone.map;


import com.eightjo.carrotclone.global.dto.http.ResponseMessage;
import com.eightjo.carrotclone.global.dto.http.StatusCode;
import com.eightjo.carrotclone.global.exception.CustomException;
import com.eightjo.carrotclone.global.security.UserDetailsImpl;
import com.eightjo.carrotclone.global.validator.MemberValidator;
import com.eightjo.carrotclone.map.Dto.*;
import com.eightjo.carrotclone.map.config.MapConfig;
import com.eightjo.carrotclone.member.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MapService {

    private final MapConfig mapConfig;
    private final MapRepository mapRepository;
    private final MemberValidator memberValidator;
    private final NearAddressRepository nearAddressRepository;
    private static RestTemplate restTemplate = new RestTemplate();

    public MapResponseDto getAddress(KakaoMapRequestDto kakaoMapRequestDto) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(MapConfig.KEY_NAME, MapConfig.KEY_PREFIX + mapConfig.getApiKey());

        ResponseEntity<KakaoMapResponseDto> responseEntity = restTemplate.exchange(
                MapConfig.MAP_URL_F + "x=" + kakaoMapRequestDto.getX() + "&y=" + kakaoMapRequestDto.getY()+MapConfig.MAP_URL_L,
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                KakaoMapResponseDto.class);

        KakaoMapResponseDto kakaoMapResponseDto = responseEntity.getBody();
        if(kakaoMapResponseDto == null){
            throw new CustomException(ResponseMessage.KAKAO_GET_ADDRESS_FAIL,StatusCode.METHOD_NOT_ALLOWED);
        }
        if(kakaoMapResponseDto.getDocuments().isEmpty()){
            throw new CustomException(ResponseMessage.KAKAO_GET_ADDRESS_FAIL,StatusCode.METHOD_NOT_ALLOWED);
        }
        Documents documents = kakaoMapResponseDto.getDocuments().get(0);

        return new MapResponseDto(
                documents.getAddress().getRegion1depthName(),
                documents.getAddress().getRegion2depthName(),
                documents.getAddress().getRegion3depthName());
    }

    @Transactional
    public void setAddressList(Address address, int size) {
        if ( size < 0 || 3 < size ) {
            throw new CustomException(ResponseMessage.WRONG_FORMAT, HttpStatus.BAD_REQUEST.value());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add(MapConfig.KEY_NAME, MapConfig.KEY_PREFIX + mapConfig.getApiKey());

        ResponseEntity<KakaoRegionResponseDto> responseEntity = null;

        for (int i = 0; i < size * 4; i++) {
            double x = address.getX();
            double y = address.getY();

            if (i % 4 == 0) {
                x += MapConfig.BASIC_METER * ((int)(i/4)+1);
            }
            else if (i % 4 == 1) {
                x -= MapConfig.BASIC_METER * ((int)(i/4)+1);
            }
            else if (i % 4 == 2) {
                y += MapConfig.BASIC_METER * ((int)(i/4)+1);
            }
            else {
                y -= MapConfig.BASIC_METER * ((int)(i/4)+1);
            }


            responseEntity = restTemplate.exchange(
                    MapConfig.REGION_URL + "x=" + x + "&y=" + y,
                    HttpMethod.GET,
                    new HttpEntity<>(null, headers),
                    KakaoRegionResponseDto.class);

            KakaoRegionResponseDto regionResponseDto = responseEntity.getBody();
            KakaoRegionResponseDto.RegionDocuments documents = regionResponseDto.getDocuments().get(1);

            Optional<Address> findAddress =  mapRepository.findByRegion1depthNameAndRegion2depthNameAndRegion3depthName(
                    documents.getRegion1depthName(),
                    documents.getRegion2depthName(),
                    documents.getRegion3depthName());
            if (findAddress.isPresent()){
                Optional<NearAddress> nearAddress = nearAddressRepository.findByParentIdAndChildId(address.getId(),findAddress.get().getId());
                if (nearAddress.isEmpty()) {
                    nearAddressRepository.save(new NearAddress(address, findAddress.get()));
                }
                else {
                    nearAddress.get().update();
                }

            }else {
                Address newAddress = new Address(
                        documents.getRegion1depthName(),
                        documents.getRegion2depthName(),
                        documents.getRegion3depthName(),
                        documents.getX(),
                        documents.getY());
                mapRepository.save(newAddress);
                Optional<NearAddress> nearAddress = nearAddressRepository.findByParentIdAndChildId(address.getId(),newAddress.getId());

                if (nearAddress.isEmpty()) {
                    nearAddressRepository.save(new NearAddress(address, newAddress));
                }
                else {
                    nearAddress.get().update();
                }
            }
        }
    }

    public KakaoMapRequestDto validAddressXY(MapRequestDto mapRequestDto) {
        KakaoAddressResponseDto kakaoAddressResponseDto = validAddress(mapRequestDto);

        return new KakaoMapRequestDto(
                kakaoAddressResponseDto.getDocuments().get(0).getX(),
                kakaoAddressResponseDto.getDocuments().get(0).getY());
    }

    public void validAddressApi(MapRequestDto mapRequestDto) {
        validAddress(mapRequestDto);
    }

    private KakaoAddressResponseDto validAddress(MapRequestDto mapRequestDto) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(MapConfig.KEY_NAME, MapConfig.KEY_PREFIX + mapConfig.getApiKey());

        ResponseEntity<KakaoAddressResponseDto> responseEntity = restTemplate.exchange(
                MapConfig.ADDRESS_URL+ mapRequestDto.getRegion1depthName()+ mapRequestDto.getRegion2depthName()+ mapRequestDto.getRegion3depthName(),
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                KakaoAddressResponseDto.class);

        KakaoAddressResponseDto kakaoAddressResponseDto = responseEntity.getBody();

        if (kakaoAddressResponseDto.getMeta().getTotalCount() == 0)
            throw new CustomException(ResponseMessage.KAKAO_GET_ADDRESS_FAIL, StatusCode.METHOD_NOT_ALLOWED);
        return kakaoAddressResponseDto;
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Object> checkAddress(String region1depthName, String region2depthName, String region3depthName) {
        Optional<Address> optionalAddress = mapRepository.findByRegion1depthNameAndRegion2depthNameAndRegion3depthName(
                region1depthName,
                region2depthName,
                region3depthName);

        if (optionalAddress.isPresent()) {
            return ResponseEntity.ok(null);
        }

        validAddress(new MapRequestDto(region1depthName, region2depthName, region3depthName));
        return ResponseEntity.ok(null);
    }

    @Transactional
    public List<DefaultAddressDto> setAddressList(Integer size, UserDetailsImpl userDetails) {
        Member member = memberValidator.validateMember(userDetails.getMember().getUserId());
        Address address = mapRepository.findById(member.getAddress().getId()).orElseThrow(
                () -> new CustomException(ResponseMessage.KAKAO_GET_ADDRESS_FAIL, HttpStatus.BAD_REQUEST.value())
        );
        List<NearAddress> nearAddresses = nearAddressRepository.findAllByParentId(address.getId());

        for (NearAddress na:nearAddresses) {
            if (na.getParent() == na.getChild()){
                continue;
            }
            na.remove();
            nearAddressRepository.deleteById(na.getId());
        }

        setAddressList(address, size);

        List<Address> nearAddressList = address.getNearChildAddress().stream().map(NearAddress::getChild).toList();
        return nearAddressList.stream().map(DefaultAddressDto::new).collect(Collectors.toList());
    }
}
