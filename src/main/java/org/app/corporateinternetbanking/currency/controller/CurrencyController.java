package org.app.corporateinternetbanking.currency.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.app.corporateinternetbanking.commons.response.GenericResponse;
import org.app.corporateinternetbanking.currency.dto.CurrencyRequest;
import org.app.corporateinternetbanking.currency.dto.CurrencyResponse;
import org.app.corporateinternetbanking.currency.enums.CurrencyStatus;
import org.app.corporateinternetbanking.currency.exceptions.CurrencyNotFound;
import org.app.corporateinternetbanking.currency.service.CurrencyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/currencies")
@Tag(name = "Currency API", description = "Handles currencies")

public class CurrencyController {
    @Autowired
    CurrencyService currencyService;

    @Operation(summary = "Change the status of a currency")
    @PostMapping("/status")
    public ResponseEntity<GenericResponse> changeStatus(@RequestBody CurrencyRequest request) throws CurrencyNotFound {
        CurrencyResponse response = currencyService.changeCurrencyStatus(request);
        return new ResponseEntity<>(GenericResponse.success(response, "Status successfully changed"), HttpStatus.OK);
    }

    @GetMapping("/all")
    @Operation(summary = "View all currencies")
    public ResponseEntity<GenericResponse> viewAll() {
        List<CurrencyResponse> response = currencyService.viewAll();
        return new ResponseEntity<>(
                GenericResponse.success(response, "Currencies found"), HttpStatus.OK);
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "View currencies by status")
    public ResponseEntity<GenericResponse> viewByStatus(
            @PathVariable CurrencyStatus status) {
        List<CurrencyResponse> response = currencyService.viewByStatus(status);
        return new ResponseEntity<>(
                GenericResponse.success(response, "Currencies found"), HttpStatus.OK);
    }
}
