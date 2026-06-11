package org.project.controller;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.project.model.AppUser;
import org.project.model.UserRole;
import org.project.repository.UserRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/")
    public String home() {
        return "home";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("user", new AppUser());
        model.addAttribute("roles", new UserRole[]{
                UserRole.LECTURER,
                UserRole.CLASS_REPRESENTATIVE,
                UserRole.CLUB_REPRESENTATIVE,
                UserRole.STUDENT
        });
        return "register";
    }

    @PostMapping("/register")
    public String createAccount(@Valid @ModelAttribute("user") AppUser user,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes,
                                HttpServletRequest request) {
        if (userRepository.existsByEmailIgnoreCase(user.getEmail())) {
            bindingResult.rejectValue("email", "duplicate", "An account already exists for this email");
        }

        if (user.getRole() == UserRole.STUDENT) {
            bindingResult.rejectValue("role", "notAllowed", "Ordinary students are not allowed to book rooms.");
        }

        if (user.getRole() == UserRole.CLASS_REPRESENTATIVE) {
            String lecturerEmail = user.getRecommendedByLecturerEmail();
            if (lecturerEmail == null || lecturerEmail.isBlank()) {
                bindingResult.rejectValue("recommendedByLecturerEmail", "required",
                        "Class representatives must provide the recommending lecturer email.");
            } else if (!userRepository.existsByEmailIgnoreCaseAndRole(lecturerEmail, UserRole.LECTURER)) {
                bindingResult.rejectValue("recommendedByLecturerEmail", "invalid",
                        "The recommending lecturer must already have a lecturer account.");
            }
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("roles", new UserRole[]{
                    UserRole.LECTURER,
                    UserRole.CLASS_REPRESENTATIVE,
                    UserRole.CLUB_REPRESENTATIVE,
                    UserRole.STUDENT
            });
            return "register";
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        AppUser savedUser = userRepository.save(user);
        signIn(savedUser, request);
        redirectAttributes.addFlashAttribute("successMessage", "Account created. Welcome to your dashboard.");
        return "redirect:/rooms";
    }

    private void signIn(AppUser user, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                null,
                java.util.List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
        request.getSession(true).setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                securityContext
        );
    }
}
