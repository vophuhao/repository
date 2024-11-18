package vn.iostar.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller

@RequestMapping("/admin/home")
public class adminController {
	
	@GetMapping("")
	public String homeAdmin() {
		
		return "admin/homeAdmin";
	}
}
