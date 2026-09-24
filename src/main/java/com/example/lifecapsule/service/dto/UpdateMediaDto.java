package com.example.lifecapsule.service.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateMediaDto {
    @Size(max = 300, message = "Izoh 300 belgidan oshmasligi kerak")
    private String caption;
}
