package com.example.teams.ms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private String id;
    private String displayName;
    private String mail;
    private String userPrincipalName;
    private String jobTitle;
    private String department;
    private String officeLocation;
    private java.util.List<String> businessPhones;
}

