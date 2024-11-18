package vn.iostar.controller;

import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.Random;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import vn.iostar.entity.ForgotPassword;
import vn.iostar.entity.UserInfo;
import vn.iostar.model.ChangePassword;
import vn.iostar.model.MailBody;
import vn.iostar.repository.ForgotPasswordRepository;
import vn.iostar.repository.UserInfoRepository;
import vn.iostar.service.EmailService;

@Controller
@RequestMapping("/forgotPassword")
public class ForgotPasswordController {

	private final UserInfoRepository userRepository;
	private final EmailService emailService;
	private final ForgotPasswordRepository forgotPasswordRepository;
	private final PasswordEncoder passwordEncoder;

	public ForgotPasswordController(UserInfoRepository userRepository, EmailService emailService, ForgotPasswordRepository forgotPasswordRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.emailService = emailService;
		this.forgotPasswordRepository = forgotPasswordRepository;
		this.passwordEncoder = passwordEncoder;
	}
	
    // Trang để người dùng nhập email để nhận OTP
    @GetMapping("/verifyMail")
    public String showVerifyMailPage() {
        return "forgot-password";
    }

    // Trang để người dùng nhập OTP
    @GetMapping("/verifyOtp")
    public String showVerifyOtpPage() {
        return "verify-otp";
    }
    
    // Trang để người dùng nhập mật khẩu mới
    @GetMapping("/changePassword")
    public String showChangePasswordPage() {
        return "change-password";
    }
    
	// send mail for email verification
    @PostMapping("/verifyMail")
    public String verifyEmail(@RequestParam("email") String email, Model model) {
        UserInfo user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Please provide a valid email!"));
        
        int otp = otpGenerator();
        MailBody mailBody = MailBody.builder()
                .to(email)
                .text("This is the OTP for your Forgot Password request: " + otp)
                .subject("OTP for Forgot Password request")
                .build();

        // Kiểm tra nếu đã tồn tại bản ghi ForgotPassword cho user
        ForgotPassword fp = forgotPasswordRepository.findByUser(user)
                .orElse(ForgotPassword.builder().user(user).build());
        
        // Cập nhật OTP và thời gian hết hạn
        fp.setOtp(otp);
        fp.setExpirationTime(new Date(System.currentTimeMillis() + 30 * 1000));
        
        emailService.sendSimpleMessage(mailBody);
        forgotPasswordRepository.save(fp);
        
        //model.addAttribute("message", "Email sent for verification!");
        model.addAttribute("email", email);
        return "redirect:/forgotPassword/verifyOtp?email=" + email;
    }
	
    // Xác thực OTP
    @PostMapping("/verifyOtp")
    public String verifyOtp(@RequestParam("otp") Integer otp, @RequestParam("email") String email, Model model) {
        UserInfo user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Please provide a valid email!"));
        
        ForgotPassword fp = forgotPasswordRepository.findByOtpAndUser(otp, user)
                .orElseThrow(() -> new RuntimeException("Invalid OTP for email: " + email));
        
        if (fp.getExpirationTime().before(Date.from(Instant.now()))) {
            forgotPasswordRepository.deleteById(fp.getFpid());
            model.addAttribute("error", "OTP has expired!");
            return "verify-otp";
        }
        
        //model.addAttribute("message", "OTP verified!");
        model.addAttribute("email", email);
        return "redirect:/forgotPassword/changePassword?email=" + email; // Chuyển hướng đến trang đổi mật khẩu
    }
	
    // Đổi mật khẩu
    @PostMapping("/changePassword")
    public String changePasswordHandler(@RequestParam("email") String email,
                                        @RequestParam("password") String password,
                                        @RequestParam("repeatPassword") String repeatPassword,
                                        Model model) {
        if (!password.equals(repeatPassword)) {
            model.addAttribute("error", "Please enter the password again!");
            return "change-password";
        }
        
        String encodedPassword = passwordEncoder.encode(password);
        userRepository.updatePassword(email, encodedPassword);
        
        model.addAttribute("message", "Password has been changed!");
        return "redirect:/login"; // Chuyển hướng đến trang login sau khi đổi mật khẩu thành công
    }
    
    private Integer otpGenerator() {
        Random random = new Random();
        return random.nextInt(100_000, 999_999);
    }
}

//@RestController
//@RequestMapping("/forgotPassword")
//public class ForgotPasswordController {
//
//	private final UserInfoRepository userRepository;
//	private final EmailService emailService;
//	private final ForgotPasswordRepository forgotPasswordRepository;
//	private final PasswordEncoder passwordEncoder;
//
//	public ForgotPasswordController(UserInfoRepository userRepository, EmailService emailService, ForgotPasswordRepository forgotPasswordRepository, PasswordEncoder passwordEncoder) {
//		this.userRepository = userRepository;
//		this.emailService = emailService;
//		this.forgotPasswordRepository = forgotPasswordRepository;
//		this.passwordEncoder = passwordEncoder;
//	}
//
//	// send mail for email verification
//	@PostMapping("/verifyMail/{email}")
//	public ResponseEntity<String> verifyEmail(@PathVariable String email) {
//		UserInfo user = userRepository.findByEmail(email)
//				.orElseThrow(() -> new UsernameNotFoundException("Please provide an valid email!"));
//		System.out.println(user);
//		int otp = otpGenerator();
//		MailBody mailBody = MailBody.builder()
//				.to(email)
//				.text("This is the OTP for your Forgot Password request:" + otp)
//				.subject("OTP for Forgot Password request")
//				.build();
//		System.out.println(mailBody);
//		ForgotPassword fp = ForgotPassword.builder()
//				.otp(otp)
//				.expirationTime(new Date(System.currentTimeMillis() + 30 * 1000))
//				.user(user)
//				.build();
//		
//		emailService.sendSimpleMessage(mailBody);
//		forgotPasswordRepository.save(fp);
//		
//		return ResponseEntity.ok("Email send for verification !");
//	}
//	
//	@PostMapping("/verifyOtp/{otp}/{email}")
//	public ResponseEntity<String> verifyOtp(@PathVariable Integer otp, @PathVariable String email){
//		UserInfo user = userRepository.findByEmail(email)
//				.orElseThrow(() -> new UsernameNotFoundException("Please provide an valid email!"));
//		
//		ForgotPassword fp = forgotPasswordRepository.findByOtpAndUser(otp, user)
//				.orElseThrow(() -> new RuntimeException("Invalid OTP for email: " + email));
//		
//		if (fp.getExpirationTime().before(Date.from(Instant.now()))) {
//			forgotPasswordRepository.deleteById(fp.getFpid());
//			return new ResponseEntity<>("OTP has expired!", HttpStatus.EXPECTATION_FAILED);
//		}
//		
//		return ResponseEntity.ok("OTP verified !");
//	}
//	
//	@PostMapping("/changePassword/{email}")
//	public ResponseEntity<String> changePasswordHandler(@RequestBody ChangePassword changePassword,
//														@PathVariable String email) {
//		if (!Objects.equals(changePassword.password(), changePassword.repeatPassword())) {
//			return new ResponseEntity<>("Please enter the password again!", HttpStatus.EXPECTATION_FAILED);
//		}
//		
//		String encodedPassword = passwordEncoder.encode(changePassword.password());
//		userRepository.updatePassword(email, encodedPassword);
//		
//		return ResponseEntity.ok("Password has been changed !");
//	}
//	
//	private Integer otpGenerator() {
//		Random random = new Random();
//		return random.nextInt(100_000, 999_999);
//	}
//}
