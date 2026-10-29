package com.inventory.manager.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponseDto {
    private String accessToken;
    private String tokenType = "Bearer";
    private String username;
    private String role;
    public JwtResponseDto(String token){
        this.accessToken = token;
    }
}
