package com.nimbleways.springboilerplate.controllers;

import com.nimbleways.springboilerplate.entities.Order;
import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.repositories.OrderRepository;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import com.nimbleways.springboilerplate.services.implementations.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

//import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
// import com.fasterxml.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Specify the controller class you want to test
// This indicates to spring boot to only load UsersController into the context
// Which allows a better performance and needs to do less mocks
@SpringBootTest
@AutoConfigureMockMvc
public class MyControllerIntegrationTests {
        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private NotificationService notificationService;

        @Autowired
        private OrderRepository orderRepository;

        @Autowired
        private ProductRepository productRepository;

        @Test
        public void processOrderShouldReturn() throws Exception {
                List<Product> allProducts = createProducts();
                Set<Product> orderItems = new HashSet<Product>(allProducts);
                Order order = createOrder(orderItems);
                productRepository.saveAll(allProducts);
                order = orderRepository.save(order);
                mockMvc.perform(post("/orders/{orderId}/processOrder", order.getId())
                                .contentType("application/json"))
                                .andExpect(status().isOk());
                Order resultOrder = orderRepository.findById(order.getId()).get();
                assertEquals(resultOrder.getId(), order.getId());
        }

        private static Order createOrder(Set<Product> products) {
                Order order = new Order();
                order.setItems(products);
                return order;
        }

        private static List<Product> createProducts() {
                List<Product> products = new ArrayList<>();
                products.add(new Product(null, 15, 30, "NORMAL", "USB Cable", null, null, null));
                products.add(new Product(null, 10, 0, "NORMAL", "USB Dongle", null, null, null));
                products.add(new Product(null, 15, 30, "EXPIRABLE", "Butter", LocalDate.now().plusDays(26), null,
                                null));
                products.add(new Product(null, 90, 6, "EXPIRABLE", "Milk", LocalDate.now().minusDays(2), null, null));
                products.add(new Product(null, 15, 30, "SEASONAL", "Watermelon", null, LocalDate.now().minusDays(2),
                                LocalDate.now().plusDays(58)));
                products.add(new Product(null, 15, 30, "SEASONAL", "Grapes", null, LocalDate.now().plusDays(180),
                                LocalDate.now().plusDays(240)));
                return products;
        }


      //ADDED BY MAINTAINER ENNASRY ABDELAZIZ

   //NORMAL avec stock disponible
    @Test
    void shouldDecrementAvailableForNormalProduct() throws Exception {

        Product product = new Product(
                null,
                5,
                10,
                "NORMAL",
                "USB Cable",
                null,
                null,
                null);

        product = productRepository.save(product);

        Order order = createOrder(Set.of(product));
        order = orderRepository.save(order);

        mockMvc.perform(post("/orders/{orderId}/processOrder", order.getId()))
                .andExpect(status().isOk());

        Product updated = productRepository.findById(product.getId()).orElseThrow();

        assertEquals(4, updated.getAvailable());
    }

    //NORMAL rupture de stock ? notification d�lai
    @Test
    void shouldNotifyDelayForNormalProductOutOfStock() throws Exception {

        Product product = new Product(
                null,
                0,
                5,
                "NORMAL",
                "USB Cable",
                null,
                null,
                null);

        product = productRepository.save(product);

        Order order = createOrder(Set.of(product));
        order = orderRepository.save(order);

        mockMvc.perform(post("/orders/{orderId}/processOrder", order.getId()))
                .andExpect(status().isOk());

        verify(notificationService).sendDelayNotification( 5, product.getName());
    }

    //EXPIRABLE valide
    @Test
    void shouldSellNonExpiredProduct() throws Exception {

        Product product = new Product(
                null,
                10,
                5,
                "EXPIRABLE",
                "Butter",
                LocalDate.now().plusDays(10),
                null,
                null);

        product = productRepository.save(product);

        Order order = createOrder(Set.of(product));
        order = orderRepository.save(order);

        mockMvc.perform(post("/orders/{orderId}/processOrder", order.getId()))
                .andExpect(status().isOk());

        Product updated = productRepository.findById(product.getId()).orElseThrow();

        assertEquals(9, updated.getAvailable());
    }

    //EXPIRABLE expir�
    @Test
    void shouldHandleExpiredProduct() throws Exception {

        Product product = new Product(
                null,
                10,
                5,
                "EXPIRABLE",
                "Milk",
                LocalDate.now().minusDays(1),
                null,
                null);

        product = productRepository.save(product);

        Order order = createOrder(Set.of(product));
        order = orderRepository.save(order);

        mockMvc.perform(post("/orders/{orderId}/processOrder", order.getId()))
                .andExpect(status().isOk());

        Product updated = productRepository.findById(product.getId()).orElseThrow();

        assertEquals(10, updated.getAvailable());
    }

    //SEASONAL pendant la saison
    @Test
    void shouldSellSeasonalProductDuringSeason() throws Exception {

        Product product = new Product(
                null,
                8,
                10,
                "SEASONAL",
                "Watermelon",
                null,
                LocalDate.now().minusDays(5),
                LocalDate.now().plusDays(5));

        product = productRepository.save(product);

        Order order = createOrder(Set.of(product));
        order = orderRepository.save(order);

        mockMvc.perform(post("/orders/{orderId}/processOrder", order.getId()))
                .andExpect(status().isOk());

        Product updated = productRepository.findById(product.getId()).orElseThrow();

        assertEquals(7, updated.getAvailable());
    }

    //SEASONAL hors saison
    @Test
    void shouldNotSellSeasonalProductOutsideSeason() throws Exception {

        Product product = new Product(
                null,
                8,
                10,
                "SEASONAL",
                "Grapes",
                null,
                LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(20));

        product = productRepository.save(product);

        Order order = createOrder(Set.of(product));
        order = orderRepository.save(order);

        mockMvc.perform(post("/orders/{orderId}/processOrder", order.getId()))
                .andExpect(status().isOk());

        Product updated = productRepository.findById(product.getId()).orElseThrow();

        assertEquals(8, updated.getAvailable());
    }

    // V�rifier le traitement global de ton dataset actuel
    @Test
    void shouldProcessAllProductsAccordingToBusinessRules() throws Exception {

        List<Product> products = createProducts();

        productRepository.saveAll(products);

        Order order = createOrder(new HashSet<>(products));
        order = orderRepository.save(order);

        mockMvc.perform(post("/orders/{orderId}/processOrder", order.getId()))
                .andExpect(status().isOk());

        Product usbCable = productRepository.findById(products.get(0).getId()).orElseThrow();
        Product butter = productRepository.findById(products.get(2).getId()).orElseThrow();
        Product watermelon = productRepository.findById(products.get(4).getId()).orElseThrow();

        assertEquals(14, usbCable.getAvailable());
        assertEquals(14, butter.getAvailable());
        assertEquals(14, watermelon.getAvailable());
    }


}
