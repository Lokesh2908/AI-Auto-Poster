package com.aiautoposter.service;

import com.aiautoposter.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    
    @Autowired
    private UserService userService;
    
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userService.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        
        return new org.springframework.security.core.userdetails.User(
            user.getEmail(),
            user.getPassword(),
            user.getIsActive(),
            true,
            true,
            true,
            getAuthorities(user.getRole())
        );
    }
    
    private Collection<? extends GrantedAuthority> getAuthorities(User.Role role) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name()));
        return authorities;
    }
}
