package de.hhu.propra.link.controllers;

import de.hhu.propra.link.entities.Link;
import de.hhu.propra.link.services.LinkService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.context.annotation.SessionScope;

import javax.validation.Valid;
import java.util.Optional;

@Controller
@SessionScope
public class LinkController {
    private final LinkService linkService;
    private String errorMessage;
    private String successMessage;
    private Link currentLink = new Link();

    public LinkController(LinkService linkService) {
        this.linkService = linkService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("links", linkService.allLinks());
        model.addAttribute("link", currentLink);
        model.addAttribute("error", errorMessage);
        model.addAttribute("success", successMessage);
        return "index";
    }

    @PostMapping("/")
    public String newLink(@ModelAttribute @Valid Link link, BindingResult bindingResult, Model model) {
        this.currentLink = link;

        if (bindingResult.hasErrors()) {
            setMessages("The link or abbreviation is invalid. Try another one.", null);
            model.addAttribute("links", linkService.allLinks());
            model.addAttribute("link", currentLink);
            model.addAttribute("error", errorMessage);
            model.addAttribute("success", successMessage);
            return "index";
        }

        if (link.getAbbreviation().isEmpty() && !linkService.createAbbreviation(link)) {
            setMessages("The link could not be shortened automatically. Supply an abbreviation.", null);
        } else if (linkService.findById(link.getAbbreviation()).isPresent()) {
            setMessages("The short link already exists. Try another one.", null);
        } else {
            linkService.save(link);
            setMessages(null, "Successfully added a new short link!");
            this.currentLink = new Link();
        }

        return "redirect:/";
    }

    @GetMapping("/{abbreviation}")
    public String redirectUrl(@PathVariable String abbreviation) {
        resetMessages();
        Optional<Link> link = linkService.findById(abbreviation);
        return link.map(value -> "redirect:" + value.getUrl()).orElse("redirect:/");
    }

    @PostMapping("/{abbreviation}/delete")
    public String deleteLink(@PathVariable String abbreviation) {
        Optional<Link> link = linkService.findById(abbreviation);
        if (link.isPresent()) {
            linkService.delete(link.get());
            setMessages(null, "Successfully deleted short link");
        } else {
            setMessages("Short link could not be deleted, because it was not found in the database", null);
        }
        return "redirect:/";
    }

    /**
     * Set Error and Success Messages for the frontend.
     *
     * @param errorMessage   Describe error
     * @param successMessage Send a joyful message to the user
     */
    private void setMessages(String errorMessage, String successMessage) {
        this.errorMessage = errorMessage;
        this.successMessage = successMessage;
    }

    /**
     * Reset UI Messages.
     */
    private void resetMessages() {
        setMessages(null, null);
    }
}
