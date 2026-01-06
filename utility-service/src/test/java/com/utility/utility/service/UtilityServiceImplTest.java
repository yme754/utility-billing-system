package com.utility.utility.service;

import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.utility.utility.entity.Tariff;
import com.utility.utility.entity.Utility;
import com.utility.utility.repository.TariffRepository;
import com.utility.utility.repository.UtilityRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class UtilityServiceImplTest {
	
    @Mock
    private UtilityRepository utilityRepo;

    @Mock
    private TariffRepository tariffRepo;

    private UtilityServiceImpl service;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        service = new UtilityServiceImpl(utilityRepo, tariffRepo);
    }

    @Test
    void getAllUtilities_returnsFlux() {
        Utility u = new Utility();
        when(utilityRepo.findAll()).thenReturn(Flux.just(u));

        StepVerifier.create(service.getAllUtilities())
                .expectNext(u)
                .verifyComplete();
    }


    @Test
    void addTariff_happyPath() {
        Tariff t = new Tariff();
        t.setName("Residential");

        when(tariffRepo.findByName("Residential")).thenReturn(Mono.empty());
        when(tariffRepo.save(t)).thenReturn(Mono.just(t));

        StepVerifier.create(service.addTariff(t))
                .expectNext(t)
                .verifyComplete();
    }

    @Test
    void getTariffsByUtility_returnsFlux() {
        Utility u = new Utility();
        u.setId("u1");
        when(utilityRepo.findByName("ELECTRICITY")).thenReturn(Mono.just(u));
        Tariff t = new Tariff();
        when(tariffRepo.findByUtilityId("u1")).thenReturn(Flux.just(t));

        StepVerifier.create(service.getTariffsByutility("ELECTRICITY"))
                .expectNext(t)
                .verifyComplete();
    }
}