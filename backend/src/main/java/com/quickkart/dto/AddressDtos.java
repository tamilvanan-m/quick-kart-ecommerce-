package com.quickkart.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AddressDtos {

    public record AddressRequest(
            @NotBlank(message = "Recipient name is required")
            @Size(max = 120)
            String fullName,

            @NotBlank(message = "Phone number is required")
            @Size(max = 20)
            String phone,

            @NotBlank(message = "Address line 1 is required")
            @Size(max = 255)
            String addressLine1,

            @Size(max = 255)
            String addressLine2,

            @NotBlank(message = "City is required")
            @Size(max = 100)
            String city,

            @NotBlank(message = "State is required")
            @Size(max = 100)
            String state,

            @NotBlank(message = "Postal code is required")
            @Size(max = 20)
            String postalCode,

            @Size(max = 80)
            String country,

            @Size(max = 30)
            String addressType,

            Boolean isDefault
    ) {}

    public record AddressDto(
            Long id,
            String fullName,
            String phone,
            String addressLine1,
            String addressLine2,
            String city,
            String state,
            String postalCode,
            String country,
            String addressType,
            Boolean isDefault
    ) {}
}
