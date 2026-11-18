package com.quickkart.service;

import com.quickkart.dto.AddressDtos;
import com.quickkart.entity.Address;
import com.quickkart.entity.User;
import com.quickkart.exception.ResourceNotFoundException;
import com.quickkart.mapper.EntityMapper;
import com.quickkart.repository.AddressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AddressService {

    private final AddressRepository addressRepository;
    private final AuthService authService;
    private final EntityMapper mapper;

    public AddressService(AddressRepository addressRepository, AuthService authService, EntityMapper mapper) {
        this.addressRepository = addressRepository;
        this.authService = authService;
        this.mapper = mapper;
    }

    public List<AddressDtos.AddressDto> getMyAddresses() {
        User user = authService.getAuthenticatedUser();
        return addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(user.getId())
                .stream()
                .map(mapper::toAddressDto)
                .toList();
    }

    @Transactional
    public AddressDtos.AddressDto createAddress(AddressDtos.AddressRequest req) {
        User user = authService.getAuthenticatedUser();
        return mapper.toAddressDto(saveAddressForUser(user, req));
    }

    @Transactional
    public Address saveAddressForUser(User user, AddressDtos.AddressRequest req) {
        List<Address> existing = addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(user.getId());
        boolean makeDefault = Boolean.TRUE.equals(req.isDefault()) || existing.isEmpty();

        if (makeDefault) {
            for (Address addr : existing) {
                if (Boolean.TRUE.equals(addr.getIsDefault())) {
                    addr.setIsDefault(false);
                    addressRepository.save(addr);
                }
            }
        }

        Address address = new Address();
        address.setUser(user);
        address.setFullName(req.fullName().trim());
        address.setPhone(req.phone().trim());
        address.setAddressLine1(req.addressLine1().trim());
        address.setAddressLine2(req.addressLine2() != null ? req.addressLine2().trim() : null);
        address.setCity(req.city().trim());
        address.setState(req.state().trim());
        address.setPostalCode(req.postalCode().trim());
        address.setCountry(req.country() != null && !req.country().isBlank() ? req.country().trim() : "India");
        address.setAddressType(req.addressType() != null && !req.addressType().isBlank() ? req.addressType().trim() : "HOME");
        address.setIsDefault(makeDefault);
        return addressRepository.save(address);
    }

    @Transactional
    public AddressDtos.AddressDto updateAddress(Long id, AddressDtos.AddressRequest req) {
        User user = authService.getAuthenticatedUser();
        Address address = addressRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Address not found."));

        if (Boolean.TRUE.equals(req.isDefault())) {
            addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(user.getId()).forEach(a -> {
                if (!a.getId().equals(id) && Boolean.TRUE.equals(a.getIsDefault())) {
                    a.setIsDefault(false);
                    addressRepository.save(a);
                }
            });
            address.setIsDefault(true);
        }

        address.setFullName(req.fullName().trim());
        address.setPhone(req.phone().trim());
        address.setAddressLine1(req.addressLine1().trim());
        address.setAddressLine2(req.addressLine2() != null ? req.addressLine2().trim() : null);
        address.setCity(req.city().trim());
        address.setState(req.state().trim());
        address.setPostalCode(req.postalCode().trim());
        if (req.country() != null && !req.country().isBlank()) address.setCountry(req.country().trim());
        if (req.addressType() != null && !req.addressType().isBlank()) address.setAddressType(req.addressType().trim());

        return mapper.toAddressDto(addressRepository.save(address));
    }

    @Transactional
    public void deleteAddress(Long id) {
        User user = authService.getAuthenticatedUser();
        Address address = addressRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Address not found."));
        addressRepository.delete(address);
    }
}
