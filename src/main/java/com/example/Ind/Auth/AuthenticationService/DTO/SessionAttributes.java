package com.example.Ind.Auth.AuthenticationService.DTO;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SessionAttributes {

    @JsonProperty(value = "tokenId")
    @NonNull
    private String tokenId;
}
