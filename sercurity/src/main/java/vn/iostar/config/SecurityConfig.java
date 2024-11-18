package vn.iostar.config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import vn.iostar.repository.UserInfoRepository;
import jakarta.servlet.http.Cookie;

@Configuration //Đánh dấu lớp này là một lớp 'cấu hình Spring'.
@EnableWebSecurity //Kích hoạt các tính năng bảo mật của Spring Security cho ứng dụng.
public class SecurityConfig {
	
	@Autowired
    private JwtRequestFilter jwtRequestFilter;
	
	@Autowired
	private UserInfoRepository repository; //được tiêm để sử dụng truy vấn UserInfo từ cơ sở dữ liệu.

	//Authentication
	@Bean
	UserDetailsService userDetailsService() {
		return new UserInfoService(repository);
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
	
	@Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
       return authenticationConfiguration.getAuthenticationManager();
   }

	@Bean
	AuthenticationProvider authenticationProvider() {
		//DaoAuthenticationProvider được cấu hình để sử dụng UserDetailsService và PasswordEncoder đã tạo ở trên.
		DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
		authenticationProvider.setUserDetailsService(userDetailsService());
		authenticationProvider.setPasswordEncoder(passwordEncoder());
		return authenticationProvider;
	}
	// security 6.1+
	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception { 
		http.csrf(csrf -> csrf.disable()) 
			.authorizeHttpRequests(auth -> auth 
					.requestMatchers("/admin/**").hasRole("ADMIN")
					.requestMatchers("/api/v1/admin/**").permitAll()
					.requestMatchers("/user/**").hasRole("USER")
					.requestMatchers("/register", "/forgotPassword/**", "/authenticate", "/register/**",
							"/authenticatelogin", "/css/**", "/fonts/**", "/images/**", "/js/**",
						"/admin/**", "/view/**", "/home/**","/vendor/register/**").permitAll()
					//.anyRequest().authenticated() 
			)
			.formLogin(form -> form
		            .loginPage("/login")
		            .defaultSuccessUrl("/default", true) // true ensures redirect always to /default after login
		            .failureUrl("/login?error=true")
		            .permitAll()
		    )
		.logout(logout -> logout
	                .logoutUrl("/logout")
	                .logoutSuccessHandler((request, response, authentication) -> {
	                    // Xoá cookie JWT khi logout
                    Cookie jwtCookie = new Cookie("JWT", null);
	                    jwtCookie.setPath("/"); // Áp dụng cho toàn bộ ứng dụng
	                    jwtCookie.setHttpOnly(true); // Bảo mật cho cookie
	                    jwtCookie.setMaxAge(0); // Đặt thời gian sống là 0 để xoá cookie
	                    response.addCookie(jwtCookie);
	                    
	                    // Điều hướng hoặc trả về response sau khi logout
	                    response.sendRedirect("/login?logout=true");
	                })
	                .permitAll()
            )
			.sessionManagement(session -> session
	                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)        )
			;
		
	    http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
		
		return http.build();
	}
}

