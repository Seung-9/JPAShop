package jpabook.jpashop.service;

import jakarta.persistence.EntityManager;
import jpabook.jpashop.domain.Item.Book;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class ItemUpdateTest {

    @Autowired EntityManager em;

    @Test
    public void updateTest() throws Exception {

        Book book = em.find(Book.class, 1L);

        // 변경감지 == dirty checking
        book.setName("asdf"); // 영속성 컨텍스트의 특징인 변경감지로 인해 트랜잭션 commit 시점에 알아서 update Query 문을 날려줌

        /**
         * 준영속 엔티티 = 영속성 컨텍스트가 더이상 관리하지 않는 Entity
         */


    }
}