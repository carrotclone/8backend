package com.eightjo.carrotclone.member.dto;

import com.eightjo.carrotclone.map.Dto.DefaultAddressDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class SingupRequestDto {
    @Size(min = 4, max = 10, message ="아이디의 길이는 4자 이상, 10자 이하이어야 합니다. ")
    @Pattern(regexp = "[a-z0-9]*$", message = "아이디는 소문자와 숫자로만 구성되어야 합니다. ")
    private String userId;

    @Size(min=4, max= 15, message="비밀번호의 길이는 8자에서 15자 사이입니다. ")
    @Pattern(regexp = "[a-zA-Z0-9`~!@#$%^&*()_=+|{};:,.<>/?]*$",message = "비밀번호는 영문과 숫자, 특수문자만 입력 가능합니다. ")
    private String password;

    @Size(min =5, max = 10 , message = "닉네임의 길이는 5자 이상, 10자 이하이어야 합니다.")
    private String nickname;

    @JsonProperty("address")
    private DefaultAddressDto address;
}
