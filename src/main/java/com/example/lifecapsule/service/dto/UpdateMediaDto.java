package com.example.lifecapsule.service.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class UpdateMediaDto {
    @Size(max = 300, message = "Izoh 300 belgidan oshmasligi kerak")
    private String caption;

    /**
     * IDs of other family members also shown in this item. Always replaces the full set -
     * the frontend sends the complete list on every call, not a delta.
     */
    private List<Long> taggedPersonIds;

    private boolean visibleToFamily;
}
