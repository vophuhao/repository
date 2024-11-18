package vn.iostar.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller

@RequestMapping("/user/home")
public class homeUserController {
	
	@GetMapping("")
	public String homeGuest() {
		
		return "page/homeUser";
	}
}
