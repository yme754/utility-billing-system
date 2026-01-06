package com.utility.utility.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.utility.utility.entity.Utility;
import com.utility.utility.repository.UtilityRepository;

import reactor.core.publisher.Mono;

class UtilitySeederTest {

    @Test
    void run_seedsUtilitiesWhenEmpty() {
        UtilityRepository repo = mock(UtilityRepository.class);
        when(repo.count()).thenReturn(Mono.just(0L));
        when(repo.save(any(Utility.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        UtilitySeeder seeder = new UtilitySeeder(repo);
        seeder.run();

        verify(repo, atLeastOnce()).save(any(Utility.class));
    }

    @Test
    void run_doesNothingWhenNotEmpty() {
        UtilityRepository repo = mock(UtilityRepository.class);
        when(repo.count()).thenReturn(Mono.just(5L));

        UtilitySeeder seeder = new UtilitySeeder(repo);
        seeder.run();

        verify(repo, never()).save(any());
    }
}
