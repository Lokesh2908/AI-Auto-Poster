package com.aiautoposter.controller;

import com.aiautoposter.entity.User;
import com.aiautoposter.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @PostMapping
    public ResponseEntity<User> createUser(@Valid @RequestBody User user) {
        try {
            User createdUser = userService.createUser(user);
            return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
    
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.findAll();
        return new ResponseEntity<>(users, HttpStatus.OK);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        Optional<User> user = userService.findById(id);
        return user.map(value -> new ResponseEntity<>(value, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
    
    @GetMapping("/email/{email}")
    public ResponseEntity<User> getUserByEmail(@PathVariable String email) {
        Optional<User> user = userService.findByEmail(email);
        return user.map(value -> new ResponseEntity<>(value, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
    
    @GetMapping("/manager/{managerId}")
    public ResponseEntity<List<User>> getUsersByManager(@PathVariable Long managerId) {
        List<User> users = userService.findByManagerId(managerId);
        return new ResponseEntity<>(users, HttpStatus.OK);
    }
    
    @GetMapping("/department/{department}")
    public ResponseEntity<List<User>> getUsersByDepartment(@PathVariable String department) {
        List<User> users = userService.findByDepartment(department);
        return new ResponseEntity<>(users, HttpStatus.OK);
    }
    
    @GetMapping("/role/{role}")
    public ResponseEntity<List<User>> getUsersByRole(@PathVariable User.Role role) {
        List<User> users = userService.findByRole(role);
        return new ResponseEntity<>(users, HttpStatus.OK);
    }
    
    @GetMapping("/active")
    public ResponseEntity<List<User>> getActiveUsers() {
        List<User> users = userService.findActiveUsers();
        return new ResponseEntity<>(users, HttpStatus.OK);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @Valid @RequestBody User user) {
        try {
            user.setId(id);
            User updatedUser = userService.updateUser(user);
            return new ResponseEntity<>(updatedUser, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
    
    @PutMapping("/{id}/deactivate")
    public ResponseEntity<User> deactivateUser(@PathVariable Long id) {
        try {
            User user = userService.deactivateUser(id);
            return new ResponseEntity<>(user, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    @PutMapping("/{id}/activate")
    public ResponseEntity<User> activateUser(@PathVariable Long id) {
        try {
            User user = userService.activateUser(id);
            return new ResponseEntity<>(user, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    @GetMapping("/exists/email/{email}")
    public ResponseEntity<Boolean> checkEmailExists(@PathVariable String email) {
        boolean exists = userService.existsByEmail(email);
        return new ResponseEntity<>(exists, HttpStatus.OK);
    }
    
    @GetMapping("/managers")
    public ResponseEntity<List<User>> getManagers() {
        try {
            System.out.println("=== GET MANAGERS DEBUG ===");
            List<User> managers = userService.findManagersAndAdmins();
            System.out.println("Found " + managers.size() + " managers/admins");
            for (User manager : managers) {
                System.out.println("Manager: " + manager.getEmail() + " - " + manager.getRole() + " - Active: " + manager.getIsActive());
            }
            return new ResponseEntity<>(managers, HttpStatus.OK);
        } catch (Exception e) {
            System.err.println("Error getting managers: " + e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping("/debug/hierarchy")
    public ResponseEntity<String> debugUserHierarchy() {
        try {
            System.out.println("=== USER HIERARCHY DEBUG ===");
            List<User> allUsers = userService.findAll();
            StringBuilder debug = new StringBuilder();
            debug.append("=== USER HIERARCHY DEBUG ===\n");
            debug.append("Total users: ").append(allUsers.size()).append("\n\n");
            
            for (User user : allUsers) {
                debug.append("User ID: ").append(user.getId())
                     .append(", Email: ").append(user.getEmail())
                     .append(", Role: ").append(user.getRole())
                     .append(", Manager ID: ").append(user.getManagerId())
                     .append(", Department: ").append(user.getDepartment())
                     .append(", Active: ").append(user.getIsActive())
                     .append("\n");
                     
                System.out.println("User: " + user.getEmail() + " - Role: " + user.getRole() + 
                                 " - Manager ID: " + user.getManagerId() + " - Active: " + user.getIsActive());
            }
            
            debug.append("\n=== MANAGER-USER RELATIONSHIPS ===\n");
            for (User user : allUsers) {
                if (user.getManagerId() != null) {
                    User manager = userService.findById(user.getManagerId()).orElse(null);
                    if (manager != null) {
                        debug.append("User: ").append(user.getEmail())
                             .append(" reports to Manager: ").append(manager.getEmail())
                             .append(" (ID: ").append(manager.getId()).append(")\n");
                    } else {
                        debug.append("User: ").append(user.getEmail())
                             .append(" has invalid manager ID: ").append(user.getManagerId()).append("\n");
                    }
                }
            }
            
            return new ResponseEntity<>(debug.toString(), HttpStatus.OK);
        } catch (Exception e) {
            System.err.println("Error in debug hierarchy: " + e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>("Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
