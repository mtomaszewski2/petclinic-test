/*
 * Copyright 2016-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.samples.petclinic.rest.controller;

import static java.util.Objects.isNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.mapper.PetMapper;
import org.springframework.samples.petclinic.mapper.VisitMapper;
import org.springframework.samples.petclinic.model.Owner;
import org.springframework.samples.petclinic.model.Pet;
import org.springframework.samples.petclinic.model.PetType;
import org.springframework.samples.petclinic.rest.dto.PetDto;
import org.springframework.samples.petclinic.rest.dto.PetTypeDto;
import org.springframework.samples.petclinic.service.ClinicService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import javax.validation.Valid;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * @author Vitaliy Fedoriv
 */

@RestController
@CrossOrigin(exposedHeaders = "errors, content-type")
@RequestMapping("api/pets")
public class PetRestController {

    private final ClinicService clinicService;

    private final PetMapper petMapper;

    private final VisitMapper visitMapper;

    public PetRestController(ClinicService clinicService, PetMapper petMapper, VisitMapper visitMapper) {
        this.clinicService = clinicService;
        this.petMapper = petMapper;
        this.visitMapper = visitMapper;
    }

    @PreAuthorize("hasRole(@roles.OWNER_ADMIN)")
    @RequestMapping(value = "/{petId}", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<PetDto> getPet(@PathVariable("petId") int petId) {
        PetDto pet = petMapper.toPetDto(this.clinicService.findPetById(petId));
        if (pet == null) {
            return new ResponseEntity<PetDto>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<PetDto>(pet, HttpStatus.OK);
    }

    @PreAuthorize("hasRole(@roles.OWNER_ADMIN)")
    @RequestMapping(value = "", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<Collection<PetDto>> getPets() {
        Collection<PetDto> pets = petMapper.toPetsDto(this.clinicService.findAllPets());
        if (pets.isEmpty()) {
            return new ResponseEntity<Collection<PetDto>>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<Collection<PetDto>>(pets, HttpStatus.OK);
    }

    @PreAuthorize("hasRole(@roles.OWNER_ADMIN)")
    @RequestMapping(value = "/pettypes", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<Collection<PetTypeDto>> getPetTypes() {
        return new ResponseEntity<Collection<PetTypeDto>>(petMapper.toPetTypeDtos(this.clinicService.findPetTypes()), HttpStatus.OK);
    }

    @PreAuthorize("hasRole(@roles.OWNER_ADMIN)")
    @RequestMapping(value = "/{petId}", method = RequestMethod.PUT, produces = "application/json")
    public ResponseEntity<PetDto> updatePet(@PathVariable("petId") int petId, @RequestBody @Valid PetDto pet, BindingResult bindingResult) {
        BindingErrorsResponse errors = new BindingErrorsResponse();
        HttpHeaders headers = new HttpHeaders();
        if (bindingResult.hasErrors() || (pet == null)) {
            errors.addAllErrors(bindingResult);
            headers.add("errors", errors.toJSON());
            return new ResponseEntity<>(headers, HttpStatus.BAD_REQUEST);
        }
        if(isNull(pet.getOwnerId())) {
            headers.add("errors", new Error("Missing owner id").toJson());
            return new ResponseEntity<>(headers, HttpStatus.BAD_REQUEST);
        }

        PetType type = clinicService.findPetTypeById(pet.getTypeId());
        if(isNull(type)) {
            headers.add("errors", new Error("Pet type does not exist in the system.").toJson());
            return new ResponseEntity<>(headers, HttpStatus.UNPROCESSABLE_ENTITY);
        }

        Owner owner = clinicService.findOwnerById(pet.getOwnerId());
        if(isNull(owner)) {
            headers.add("errors", new Error("Owner does not exist in the system.").toJson());
            return new ResponseEntity<>(headers, HttpStatus.UNPROCESSABLE_ENTITY);
        }

        Pet currentPet = this.clinicService.findPetById(petId);
        if (currentPet == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        currentPet.setBirthDate(pet.getBirthDate());
        currentPet.setName(pet.getName());
        currentPet.setType(clinicService.findPetTypeById(pet.getTypeId()));
        this.clinicService.savePet(currentPet);
        return new ResponseEntity<>(petMapper.toPetDto(currentPet), HttpStatus.NO_CONTENT);
    }

    @PreAuthorize("hasRole(@roles.OWNER_ADMIN)")
    @RequestMapping(value = "/", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<PetDto> addPet(@RequestBody @Valid PetDto pet, BindingResult bindingResult) {
        BindingErrorsResponse errors = new BindingErrorsResponse();
        HttpHeaders headers = new HttpHeaders();
        if (bindingResult.hasErrors() || (pet == null)) {
            errors.addAllErrors(bindingResult);
            headers.add("errors", errors.toJSON());
            return new ResponseEntity<>(headers, HttpStatus.BAD_REQUEST);
        }
        if(isNull(pet.getOwnerId())) {
            headers.add("errors", new Error("Missing owner id").toJson());
            return new ResponseEntity<>(headers, HttpStatus.BAD_REQUEST);
        }

        PetType type = clinicService.findPetTypeById(pet.getTypeId());
        if(isNull(type)) {
            headers.add("errors", new Error("Pet type does not exist in the system.").toJson());
            return new ResponseEntity<>(headers, HttpStatus.UNPROCESSABLE_ENTITY);
        }

        Owner owner = clinicService.findOwnerById(pet.getOwnerId());
        if(isNull(owner)) {
            headers.add("errors", new Error("Owner does not exist in the system.").toJson());
            return new ResponseEntity<>(headers, HttpStatus.UNPROCESSABLE_ENTITY);
        }

        Pet petEntity = new Pet();
        petEntity.setBirthDate(pet.getBirthDate());
        petEntity.setType(type);
        petEntity.setOwner(owner);
        petEntity.setName(pet.getName());

        this.clinicService.savePet(petEntity);
        return new ResponseEntity<>(petMapper.toPetDto(petEntity), HttpStatus.NO_CONTENT);
    }

    @PreAuthorize("hasRole(@roles.OWNER_ADMIN)")
    @RequestMapping(value = "/{petId}", method = RequestMethod.DELETE, produces = "application/json")
    @Transactional
    public ResponseEntity<Void> deletePet(@PathVariable("petId") int petId) {
        Pet pet = this.clinicService.findPetById(petId);
        if (pet == null) {
            return new ResponseEntity<Void>(HttpStatus.NOT_FOUND);
        }
        this.clinicService.deletePet(pet);
        return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
    }


}
