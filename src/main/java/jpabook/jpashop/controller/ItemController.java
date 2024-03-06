package jpabook.jpashop.controller;

import jpabook.jpashop.domain.Item.Book;
import jpabook.jpashop.dto.UpdateItemDto;
import jpabook.jpashop.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.hibernate.sql.Update;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @GetMapping("/items/new")
    public String createForm(Model model) {
        model.addAttribute("bookForm", new BookForm());
        return "items/createItemForm";
    }

    @PostMapping("/items/new")
    public String create(BookForm bookForm) {
        Book book = new Book();
        book.setName(bookForm.getName());
        book.setPrice(bookForm.getPrice());
        book.setStockQuantity(bookForm.getStockQuantity());
        book.setAuthor(bookForm.getAuthor());
        book.setIsbn(bookForm.getIsbn());

        itemService.save(book);
        return "redirect:/";
    }

    @GetMapping("/items")
    public String list(Model model) {
        model.addAttribute("items", itemService.findAll());
        return "items/itemList";
    }

    @GetMapping("/items/{itemId}/edit")
    public String updateItemForm(@PathVariable(name = "itemId") Long itemId, Model model) {
        Book item = (Book) itemService.findOne(itemId);

        BookForm updateForm = new BookForm();
        updateForm.setId(item.getId());
        updateForm.setName(item.getName());
        updateForm.setPrice(item.getPrice());
        updateForm.setStockQuantity(item.getStockQuantity());
        updateForm.setAuthor(item.getAuthor());
        updateForm.setIsbn(item.getIsbn());

        model.addAttribute("updateForm", updateForm);
        return "items/updateItemForm";
    }

    @PostMapping("/items/{itemId}/edit")
    public String updateItem(@PathVariable(name = "itemId") Long itemId, @ModelAttribute("updateForm") BookForm bookForm) {

        /**
         * 이게 준영속 상태의 객체임.
         * new 를 통해 구분하면 안 됨.식별자 id 를 기준으로 영속상태가 되어서 DB 에 저장이 된 적이 있는가 없는가로 따져야됨.
         * html form 에 데이터를 노출한 이후에 다시 new 로 재조립된 엔티티일 수도 있기 때문임...
         */

//        Book book = new Book();
//        book.setId(updateForm.getId()); // 이미 DB에서 한 번 저장되고 불러온 애 -> 준영속 엔티티
//        book.setName(updateForm.getName());
//        book.setPrice(updateForm.getPrice());
//        book.setStockQuantity(updateForm.getStockQuantity());
//        book.setAuthor(updateForm.getAuthor());
//        book.setIsbn(updateForm.getIsbn());
        UpdateItemDto updateItemDto = new UpdateItemDto();
        updateItemDto.setName(bookForm.getName());
        updateItemDto.setPrice(bookForm.getPrice());
        updateItemDto.setStockQuantity(bookForm.getStockQuantity());

        itemService.updateItem(itemId, updateItemDto);

        return "redirect:/items";
    }

}
