package vn.iostar.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import vn.iostar.config.UserInfoService;
import vn.iostar.entity.UserInfo;
import vn.iostar.model.AuthRequest;
import vn.iostar.repository.UserInfoRepository;
import vn.iostar.service.UserService;
import vn.iostar.util.JwtUtil;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserInfoService userDetailsService;
    
    @Autowired
    private UserInfoRepository userInfoRepository;
    
    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("userInfo", new UserInfo());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute("userInfo") @Validated UserInfo userInfo, BindingResult result) {
    	// Validation
        if (result.hasErrors()) {
            return "register";
        }
        
        // Add user to Database
        userService.registerUser(userInfo);
        
        return "redirect:/register/register-verify-otp?email=" + userInfo.getEmail();
    }
    
    // Trang để người dùng nhập OTP
    @GetMapping("/register/register-verify-otp")
    public String showVerifyOtpPage() {
        return "register-verify-otp";
    }
    
    @GetMapping("/login")
    public String loginUser(@RequestParam(value = "error", required = false) String error, Model model) {
    	if (error != null) {
    		model.addAttribute("message", "Invalid username or password");
    	}
    	model.addAttribute("userModel", new AuthRequest());
    	return "login";
    }
    
    @PostMapping("/authenticate")
    public String authenticate(@RequestParam("username") String username,
                               @RequestParam("password") String password,
                               HttpServletResponse response, Model model) {
        try {
        	// Tìm người dùng trong cơ sở dữ liệu
            UserInfo user = userInfoRepository.findByName(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            // Kiểm tra xem tài khoản có được kích hoạt không
            if (!user.isEnabled()) {
                model.addAttribute("error", "Your account is disabled.");
                return "login";
            }
        	
            // Tiến hành xác thực người dùng
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
            );

            final UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            String token = jwtUtil.generateToken(userDetails.getUsername());
            
            // Thiết lập token dưới dạng cookie
            Cookie jwtCookie = new Cookie("JWT", token);
            jwtCookie.setHttpOnly(true); // Bảo mật cookie, không truy cập được từ JavaScript
            jwtCookie.setPath("/"); // Áp dụng cookie cho toàn bộ ứng dụng
            jwtCookie.setMaxAge(60 * 60 * 10); // Thời hạn 10 giờ
            response.addCookie(jwtCookie);
            
            // Lưu token vào model để truyền sang view nếu cần
            model.addAttribute("token", token);

            // Điều hướng dựa trên vai trò người dùng
            if (userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                return "redirect:/admin/home";  // Điều hướng đến trang admin
            } else if (userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER"))) {
                return "redirect:/user/home";   // Điều hướng đến trang user
            } else {
                return "redirect:/vendor/home"; // Điều hướng đến trang vendor
            }
        } catch (AuthenticationException e) {
            model.addAttribute("error", "Invalid username or password");
            return "login";  // Quay lại trang đăng nhập nếu lỗi
        }
    }
    
    // login không dùng PostMapping, Bắt buộc dùng (username, pasword) ở view 
    // để Spring Security tự động lưu vào userInfo  
    
    @PostMapping("/register/register-verify-otp")
    public String verifyOtp(@RequestParam("otp") Integer otp, @RequestParam("email") String email, Model model) {
    	UserInfo user = userInfoRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Please provide a valid email!"));

        if (user != null && user.getOtp().equals(otp)) {
            user.setEnabled(true);
            user.setOtp(null); // Clear OTP after successful verification
            userInfoRepository.save(user);
            model.addAttribute("message", "Account activated successfully!");
            return "redirect:/login";
        } else {
        	model.addAttribute("message", "Invalid OTP!");
            return "register-verify-otp";
        }
    }
    
}
