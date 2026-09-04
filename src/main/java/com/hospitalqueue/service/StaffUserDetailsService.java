package com.hospitalqueue.service;

import com.hospitalqueue.entity.StaffUser;
import com.hospitalqueue.repository.StaffUserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class StaffUserDetailsService implements UserDetailsService {

    private final StaffUserRepository staffUserRepository;

    public StaffUserDetailsService(StaffUserRepository staffUserRepository) {
        this.staffUserRepository = staffUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        StaffUser staffUser = staffUserRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("No such user: " + username));

        return User.withUsername(staffUser.getUsername())
                .password(staffUser.getPasswordHash())
                .roles(staffUser.getRole().name())
                .build();
    }
}
