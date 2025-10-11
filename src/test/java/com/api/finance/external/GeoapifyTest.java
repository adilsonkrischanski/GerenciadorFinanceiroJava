package com.api.finance.external;

import com.api.finance.external.geoapify.GeoapifyClient;
import com.api.finance.external.geoapify.GeoapifyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class GeoapifyTest {

    @Mock
    private GeoapifyClient client;

    @InjectMocks
    private GeoapifyService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        ReflectionTestUtils.setField(service, "apiKey", "FAKE_API_KEY");
    }

    @Test
    void testGetTouristAttractions() {
        double lon = -43.209;
        double lat = -22.911;
        int radius = 3000;

        String fakeResponse = "{ \"features\": [] }";

        when(client.get(anyString())).thenReturn(fakeResponse);

        String result = service.getTouristAttractions(lon, lat, radius);

        assertEquals(fakeResponse, result);
    }
}
