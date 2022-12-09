package com.orange.lo.sample.lo2iothub.modify;

import com.orange.lo.sample.lo2iothub.modify.model.ModifyConfigurationProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/config")
public class ModifyConfigurationController {

    private ModifyConfigurationService modifyConfigurationService;

    public ModifyConfigurationController(ModifyConfigurationService modifyConfigurationService) {
        this.modifyConfigurationService = modifyConfigurationService;
    }

    @GetMapping()
    public ResponseEntity<ModifyConfigurationProperties> get() {
        return ResponseEntity.ok(modifyConfigurationService.getProperties());
    }

    @PatchMapping
    public ResponseEntity<Void> modify(@RequestBody ModifyConfigurationProperties modifyConfigurationProperties) {
        modifyConfigurationService.modify(modifyConfigurationProperties);
        return ResponseEntity.noContent().build();
    }
}