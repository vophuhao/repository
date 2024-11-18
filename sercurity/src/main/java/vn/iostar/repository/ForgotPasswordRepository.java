package vn.iostar.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import vn.iostar.entity.ForgotPassword;
import vn.iostar.entity.UserInfo;

public interface ForgotPasswordRepository extends JpaRepository<ForgotPassword, Integer> {
	
	@Query("SELECT fp FROM ForgotPassword fp WHERE fp.otp = ?1 AND fp.user = ?2")
	Optional<ForgotPassword> findByOtpAndUser(Integer otp, UserInfo user);
	
	@Query("SELECT fp FROM ForgotPassword fp WHERE fp.user = ?1")
    Optional<ForgotPassword> findByUser(UserInfo user);
}
