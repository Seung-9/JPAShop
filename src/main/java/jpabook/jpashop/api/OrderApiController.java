package jpabook.jpashop.api;

import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderItem;
import jpabook.jpashop.dto.OrderDto;
import jpabook.jpashop.dto.OrderFlatDto;
import jpabook.jpashop.dto.OrderItemQueryDto;
import jpabook.jpashop.dto.OrderQueryDto;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.OrderSearch;
import jpabook.jpashop.repository.order.query.OrderQueryRepository;
import jpabook.jpashop.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static java.util.stream.Collectors.*;

/**
 * 일대다 관계에서 최적화
 */
@RestController
@RequiredArgsConstructor
public class OrderApiController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final OrderQueryRepository orderQueryRepository;

    /**
     * 주문 조회 V1 - Entity 로 직접 조회
     */
    @GetMapping("/api/v1/orders")
    public List<Order> ordersV1(OrderSearch orderSearch) {
        List<Order> orders = orderService.findOrders(orderSearch);

        // Lazy 강제 초기화
        for (Order order : orders) {
            order.getMember().getName();
            order.getDelivery().getAddress();
            List<OrderItem> orderItems = order.getOrderItems();
            orderItems.forEach(o -> o.getItem().getName());
        }
        return orders;
    }

    /**
     * 주문 조회 V2 - Entity -> DTO 로 변환해서 조회 ( fetch join 사용 x -> N + 1 문제 )
     * DTO 안에 Entity 존재하면 안 됨. 따라서 OrderItemDto 도 생성해줘야됨
     * order 1번, member N 번, address N 번, orderItem N번 ( order 조회 수 만큼)
     * item N번 ( orderItem 조회 수 만큼 )
     */
    @GetMapping("/api/v2/orders")
    public List<OrderDto> ordersV2() {
        List<Order> orders = orderService.findOrders(new OrderSearch());

        return orders.stream()
                .map(OrderDto::new)
                .toList();
    }

    /**
     * 주문 조회 V3 - Entity -> DTO 로 변환해서 조회 ( fetch join 사용 o -> Query 한 번만 날라감)
     * fetch join - 컬렉션 추가
     * distinct 를 사용한 이유는 1대다 조인이 있으므로 데이터베이스 row가 증가 -> 엔티티의 조회 수도 증가
     * JPA의 distinct는 SQL에 distinct를 추가하고, 더해서 같은 엔티티가 조회되면 -> 중복을 걸러준다
     * 근데 컬렉션을 fetch join 하면 페이징 불가능 -> 모든 데이터를 DB 에서 읽어오고, 메모리에서 페이징해버림
     * 컬렉션 페치 조인은 1개만 사용할 수 있다.
     * 컬렉션 둘 이상에 페치 조인을 사용하면 안된다. 데이터가 부정합하게 조회될 수 있다.(1 - N , 1 - N), (1 - N - M)
     */
    @GetMapping("/api/v3/orders")
    public List<OrderDto> ordersV3() {
        List<Order> orders = orderRepository.findAllWithItem();

        return orders.stream()
                .map(OrderDto::new)
                .toList();
    }

    /**
     * 주문 조회 V3.1 - Entity -> DTO 변환 페이
     * ToOne 관계만 우선 모두 페치 조인으로 최적화
     * 일대다 관계에서 패치조인을 진행하면 row가 증가해버림.. 일을 기준으로 하는 게 아니라 다를 기준으로 조인이 되기 때문
     * 컬렉션 관계는 hibernate.default_batch_fetch_size, @BatchSize로 최적화
     * in 절에 batchsize가 들어가게 됨 -> 쿼리가 추가적으로 2번 ( orderItem, item ) 으로 최적화됨
     */
    @GetMapping("/api/v3.1/orders")
    public List<OrderDto> ordersV3_page(@RequestParam(value = "offset", defaultValue = "0") int offset,
                                        @RequestParam(value = "limit", defaultValue = "100") int limit) {

        List<Order> orders = orderRepository.findAllWithMemberDelivery(offset, limit); // toOne 관계만 fetch join

        return orders.stream()
                .map(OrderDto::new)
                .toList();
    }

    /**
     * 주문 조회 V4 - JPA 에서 DTO 직접 조회
     * ToOne(N:1, 1:1) 관계들을 먼저 조회, ToMany(1:N) 관계는 각각 별도 처리
     * ToOne 관계는 조인해도 데이터 row 수가 증가하지 않음. ToMany(1:N) 관계는 조인하면 row 수가 증가
     * row 수가 증가하지 않는 ToOne 관계는 조인으로 최적화 하기 쉽기 때문에 한번에 조회하고,
     * ToMany 관계 는 최적화 하기 어려우므로 findOrderItems() 같은 별도의 메서드로 조회
     * 근데 어짜피 1 + N 번의 쿼리가 발생함.
     */
    @GetMapping("/api/v4/orders")
    public List<OrderQueryDto> ordersV4() {
        return orderQueryRepository.findOrderQueryDtos();
    }

    /**
     * 주문 조회 V5 - JPA 에서 DTO 직접 조회 - 컬렉션 조회 최적화 ( ToMany 최적화 )
     * ToOne 관계들을 먼저 조회하고, orderId 로  ToMany 관계인 orderItems 를 한 꺼번에 조회하면 1번, 1번 씩만 조회됨.
     * MAP 을 사용해서 성능 향상 - O(1)
     */
    @GetMapping("/api/v5/orders")
    public List<OrderQueryDto> ordersV5() {
        return orderQueryRepository.findAllByDto_optimization();
    }

    /**
     * 주문 조회 V6 - JPA 에서 DTO 직접 조회 - 플랫 데이터 최적화 쿼리 한 번 나감
     * 근데 쿼리는 한 번 나가긴 하는데 중복 데이터가로 인해서 V5 보다 느릴 수도 있음
     * 대신 페이징 불가능
     */
    @GetMapping("/api/v6/orders")
    public List<OrderQueryDto> ordersV6() {
        List<OrderFlatDto> flats = orderQueryRepository.findAllByDto_flat(); // 중복 제거하지 않고 모두 조인해서 조회

        // 중복 제거하고 OrderFlatDto -> OrderQueryDto 로 변환
        return flats.stream()
                .collect(groupingBy(o -> new OrderQueryDto(o.getOrderId(), o.getName(), o.getOrderDate(), o.getOrderStatus(), o.getAddress()),
                        mapping(o -> new OrderItemQueryDto(o.getOrderId(), o.getItemName(), o.getOrderPrice(), o.getCount()), toList())))
                .entrySet().stream()
                .map(e -> new OrderQueryDto(e.getKey().getOrderId(), e.getKey().getName(), e.getKey().getOrderDate(),
                        e.getKey().getOrderStatus(), e.getKey().getAddress(), e.getValue()))
                .toList();
    }
}
