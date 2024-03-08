package jpabook.jpashop.api;

import jpabook.jpashop.domain.Order;
import jpabook.jpashop.dto.SimpleOrderDto;
import jpabook.jpashop.repository.order.simplequery.OrderSimpleQueryDto;
import jpabook.jpashop.repository.OrderSearch;
import jpabook.jpashop.repository.order.simplequery.OrderSimpleQueryRepository;
import jpabook.jpashop.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class OrderSimpleApiController {

    private final OrderService orderService;
    private final OrderSimpleQueryRepository orderSimpleQueryRepository;

    /**
     * 주문 하기 V1 - 엔티티
     */
    @GetMapping("/api/v1/simple-orders")
    public List<Order> ordersV1() {
        List<Order> all = orderService.findOrders(new OrderSearch());
        // Lazy 강제 초기화
        // 양방향 관계로 인한 무한 참조 문제로 Order의 member 필드 혹은 Member의 orders 필드에 @JsonIgnore 를 붙어야됨
        // Jackson 라이브러리는 지연로딩으로 인해 생성된 프록시 객체를 Json 으로 생성을 못해서 예외가 발생하기 때문에 초기화를 시켜줘야됨
        // 초기화를 시키면 객체가 들어가게 됨. 따라서 직렬화 가능해짐
        // Hibernate5Module 을 스프링 빈에 등록 -> 초기화된 프록시 객체만 노출, 초기화 되지 않은 프록시 객체는 노출 안함.
        for (Order order : all) {
            order.getMember().getName();
            order.getDelivery().getAddress();
        }
        return all;
    }

    /**
     * 주문하기 V2 ( fetch join 사용 x ) - Entity -> DTO 변환
     * 지연 로딩으로 인해 쿼리 N 번 호출함
     * order 1번, order -> member 지연로딩 N번, order -> delivery 지연로딩 N 번
     * 만약 order가 10개가 있으면 최악의 경우 1 + 10 + 10 번 실행됨.
     */
    @GetMapping("/api/v2/simple-orders")
    public List<SimpleOrderDto> ordersV2() {
        List<Order> orders = orderService.findOrders(new OrderSearch());

        return orders.stream()
                .map(SimpleOrderDto::new)
                .toList();
    }

    /**
     * 주문하기 V3 ( fetch join 사용 o ) -> Entity -> DTO 변환
     */
    @GetMapping("/api/v3/simple-orders")
    public List<SimpleOrderDto> ordersV3() {
        // fetch join -> lazy 다 무시, proxy도 아닌 진짜 객체 값을 다 채워서 가져온다
        List<Order> orders = orderService.findOrdersUseFetchJoin();

        return orders.stream()
                .map(SimpleOrderDto::new)
                .toList();
    }

    /**
     * 주문하기 V4 JPA 에서 바로 DTO로 조회
     */
    @GetMapping("/api/v4/simple-orders")
    public List<OrderSimpleQueryDto> ordersV4() {
        return orderSimpleQueryRepository.findOrderDtos();
    }


    /**
     * 주문 취소
     */



    /**
     * 주문 검색
     */


}
