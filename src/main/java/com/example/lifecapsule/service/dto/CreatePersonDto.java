package com.example.lifecapsule.service.dto;

import com.example.lifecapsule.entity.enumirated.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class CreatePersonDto {
    @NotBlank(message = "Ism bo'sh bo'lishi mumkin emas")
    @Size(max = 100, message = "Ism 100 belgidan oshmasligi kerak")
    private String firstName;

    @Size(max = 100, message = "Familiya 100 belgidan oshmasligi kerak")
    private String lastName;

    @Size(max = 100, message = "Qizlik familiyasi 100 belgidan oshmasligi kerak")
    private String maidenName;

    private Gender gender;

    @PastOrPresent(message = "Tug'ilgan sana kelajakda bo'lishi mumkin emas")
    private LocalDate birthDate;

    @PastOrPresent(message = "Vafot etgan sana kelajakda bo'lishi mumkin emas")
    private LocalDate deathDate;

    @Size(max = 150, message = "Tug'ilgan joy 150 belgidan oshmasligi kerak")
    private String birthPlace;

    @Size(max = 150, message = "Kasb 150 belgidan oshmasligi kerak")
    private String occupation;

    @Size(max = 2000, message = "Biografiya 2000 belgidan oshmasligi kerak")
    private String biography;

    @Size(max = 500, message = "Rasm manzili 500 belgidan oshmasligi kerak")
    private String photoUrl;

    private Long linkedUserId;
}
