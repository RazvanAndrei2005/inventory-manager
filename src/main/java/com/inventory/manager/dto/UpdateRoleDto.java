package com.inventory.manager.dto;

import com.inventory.manager.entity.Role;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRoleDto {
    @NotNull(message = "Role cannot be null")
    private Role role;
}
