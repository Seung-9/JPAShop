package jpabook.jpashop.service;

import jpabook.jpashop.domain.Item.Item;
import jpabook.jpashop.dto.UpdateItemDto;
import jpabook.jpashop.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemService {

    private final ItemRepository itemRepository;

    @Transactional
    public void save(Item item) {
        itemRepository.save(item);
    }

    /**
     * 더티 체킹 ( 변경 감지 )
     * 이렇게만 해줘도 update 쿼리문 날라감
     * 원하는 속성만 변경이 가능
     * merge 가 이 로직 ( 단, merge 는 모든 속성을 update 함 ) -> 가급적이면 merge 안 쓰는 게 좋음
     */
    @Transactional
    public void updateItem(Long itemId, UpdateItemDto updateItemDto) {

        Item findItem = itemRepository.findOne(itemId);
        findItem.changeItem(updateItemDto);
    }

    public Item findOne(Long id) {
        return itemRepository.findOne(id);
    }

    public List<Item> findAll() {
        return itemRepository.findAll();
    }
}
