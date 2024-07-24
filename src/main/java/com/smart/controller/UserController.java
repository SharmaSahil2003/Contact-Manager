package com.smart.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.Path;
import java.security.Principal;
import java.util.List;
//import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.smart.dao.ContactRepository;
import com.smart.dao.UserRepository;
import com.smart.entities.Contact;
import com.smart.entities.User;
import com.smart.helper.Message;

import jakarta.servlet.http.HttpSession;


@Controller
@RequestMapping("/user")
public class UserController {

	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private ContactRepository contactRepository;
	
	@ModelAttribute
	public void addCommonData(Model model,Principal principal) {
		String userName = principal.getName();
		
		System.out.println(userName);
		
		User user = userRepository.getUserByUserName(userName);
		
		System.out.println(user);
		
		model.addAttribute("user",user);
	}
	
	@GetMapping("/index")
	public String dashboard(Model model,Principal principal) {
		model.addAttribute("title","User Dashboard");
		return "normal/user_dashboard";
	}
	
	@GetMapping("/add-contact")
	public String openAddContactForm(Model model) {
		model.addAttribute("title","Add Contact");
		model.addAttribute("contact", new Contact());
		return "normal/add_contact_form";
	}
	
	@PostMapping("/user/process-contact")
	public String processContact(@ModelAttribute Contact contact,@RequestParam("profileImage") MultipartFile file,Principal principal,HttpSession session) {
		try {
			
			String name = principal.getName();
			User user = this.userRepository.getUserByUserName(name);
			
			
			if(file.isEmpty()) {
				System.out.println("File is empty");
				contact.setImage("contact.png");
			}
			else {
				contact.setImage(file.getOriginalFilename());
				File saveFile = new ClassPathResource("static/img").getFile();
				
				Path path = Paths.get(saveFile.getAbsolutePath()+File.separator+file.getOriginalFilename());
				
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
				
				System.out.println("Image is uploaded");
			}
			
			contact.setUser(user);
			user.getContacts().add(contact);
			
			
			this.userRepository.save(user);
			
			System.out.println("Added to database");
			
			session.setAttribute("message", new Message("Your contact is added !! Add more..", "success"));
			
			
		}catch (Exception e) {
			// TODO: handle exception
			System.out.println("ERROR "+e.getMessage());
			session.setAttribute("message", new Message("Something went wrong !! Try again..", "danger"));
		}
		
		return "normal/add_contact_form";
	}
	
	@GetMapping("/show-contacts/{page}")
	public String showContacts(@PathVariable("page") Integer page,Model m,Principal principal) {
		m.addAttribute("title","Show User Contacts");
		
		String userName = principal.getName();
		
		User user = this.userRepository.getUserByUserName(userName);
		
		Pageable pageable = PageRequest.of(page, 7);
		
		Page<Contact> contacts = this.contactRepository.findContactsByUser(user.getId(),pageable);

		m.addAttribute("contacts",contacts);
		m.addAttribute("currentPage", page);
		m.addAttribute("totalPages",contacts.getTotalPages());
		
		return "normal/show_contacts";
	}
	
	@GetMapping("/contact/{cId}")
	public String showContactDetail(@PathVariable("cId") Integer cId,Model model,Principal principal) {
		System.out.println("CID :"+cId);
		
		
		String userName = principal.getName();
		
		User user = this.userRepository.getUserByUserName(userName);
		
		
		
		Optional<Contact> contactOptional = this.contactRepository.findById(cId);
		Contact contact = contactOptional.get();
		
		if(user.getId()==contact.getUser().getId()) {
			model.addAttribute("contact",contact);
		}
		return "normal/contact_detail";
	}
	
	@GetMapping("/delete/{cid}")
	public String deleteContact(@PathVariable("cid") Integer cId,Model model,Principal principal,HttpSession session) {
		
		String userName = principal.getName();
		
		User user = this.userRepository.getUserByUserName(userName);
		
		Optional<Contact> contactOptional = this.contactRepository.findById(cId);
		Contact contact = contactOptional.get();
		
		if(user.getId()==contact.getUser().getId()) {
//			contact.setUser(null);
			if(contact.getImage()!="contact.png") {
				try {
				File deleteFile = new ClassPathResource("static/img").getFile();
				File file1 = new File(deleteFile,contact.getImage());
				file1.delete();
				}
				catch (Exception e) {
					System.out.println(e.getMessage());
				}
			}
			
			List<Contact> contacts =user.getContacts();
			for(int i=0;i<contacts.size();i++) {
				if(contacts.get(i).getcId()==cId) {
					contacts.remove(contacts.get(i));
				}
			}
			this.contactRepository.delete(contact);
			session.setAttribute("message", new Message("Contact deleted successfully...", "success"));
		}
		else {
			session.setAttribute("message", new Message("You don't have permission to see this contact...", "danger"));
		}
		
		
		return "redirect:/user/show-contacts/0";
	}
	
	@PostMapping("/update-contact/{cid}")
	public String updateForm(@PathVariable("cid") Integer cid,Model model) {
		model.addAttribute("title","Update Contact");
		
		Contact contact = this.contactRepository.findById(cid).get();
		
		model.addAttribute("contact",contact);
		
		return "normal/update_form";
	}
	
	@PostMapping("/process-update")
	public String updateHandler(@ModelAttribute Contact contact,@RequestParam("profileImage") MultipartFile file,Model m,HttpSession session,Principal principal) {
		System.out.println(contact.getName());
		System.out.println(contact.getcId());
		
		try {
			
			Contact oldContactDetail = this.contactRepository.findById(contact.getcId()).get();
			
			
			if(!file.isEmpty()) {
				
				File deleteFile = new ClassPathResource("static/img").getFile();
				File file1 = new File(deleteFile,oldContactDetail.getImage());
				file1.delete();
				
				File saveFile = new ClassPathResource("static/img").getFile();
				
				Path path = Paths.get(saveFile.getAbsolutePath()+File.separator+file.getOriginalFilename());
				
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
				
				contact.setImage(file.getOriginalFilename());
			}
			else {
				contact.setImage(oldContactDetail.getImage());
			}
			
			User user=this.userRepository.getUserByUserName(principal.getName());
			
			contact.setUser(user);
			
			this.contactRepository.save(contact);
			
			session.setAttribute("message", new Message("Your Contact is updated !!", "success"));
			
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		
		
		return "redirect:/user/contact/"+contact.getcId();
	}
	
	@GetMapping("/profile")
	public String yourProfile(Model model) {
		model.addAttribute("title","Profile Page");
		return "normal/profile";
	}
	
	
}
