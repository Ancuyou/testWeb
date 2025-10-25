package it.ute.QAUTE.service;

import it.ute.QAUTE.dto.ConsultantDTO;
import it.ute.QAUTE.entity.Consultant;
import it.ute.QAUTE.entity.Profiles;
import it.ute.QAUTE.repository.ConsultantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ConsultantService {

    @Autowired
    private ConsultantRepository consultantRepository;
    
    public List<ConsultantDTO> getAllConsultants() {
        List<Consultant> consultants = consultantRepository.findAllWithProfiles();
        
        return consultants.stream().map(consultant -> {
            Profiles profile = consultant.getProfile();
            
            ConsultantDTO dto = new ConsultantDTO();
            dto.setConsultantID(consultant.getConsultantID());
            dto.setProfileID(profile.getProfileID());
            dto.setFullName(profile.getFullName());
            dto.setAvatar(profile.getAvatar());
            dto.setExperienceYears(consultant.getExperienceYears());
            dto.setIsOnline(false); 
            
            return dto;
        }).collect(Collectors.toList());
    }
    public Optional<Consultant> findByProfileId(Integer profileId) {
        return Optional.ofNullable(consultantRepository.findByProfile_ProfileID(profileId));
    }
    public List<Consultant> findAllConsultants() {
        return consultantRepository.findAllWithProfiles();
    }

    public void updateConsultant(Consultant consultant) {
        consultantRepository.save(consultant);
    }
    
    public void saveConsultant(Consultant consultant) {
        consultantRepository.save(consultant);
    }
}