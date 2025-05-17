#  Rapport Technique — TP Gestion Hospitalière avec Spring Boot

##  Introduction

Ce projet vise à développer une application de gestion hospitalière à l’aide de Spring Boot, Spring Data JPA, Lombok et MySQL. Il modélise les entités clés d’un établissement hospitalier — patients, médecins, rendez-vous et consultations — avec des relations adéquates. Une gestion de rôles utilisateurs (`ADMIN`, `USER`) est également intégrée pour ouvrir la voie à l’ajout futur d’une sécurité basée sur Spring Security.

---

## Modélisation des Entités

### 1. `Patient`

```java
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Patient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nom;
    private Date dateNaissance;
    private boolean malade;
    private int score;
}
```

### 2. `Medecin`

```java
@Entity
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Medecin {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nom;
    private String email;
    private String specialite;

    @OneToMany(mappedBy = "medecin")
    private List<RendezVous> rendezVous;
}
```

### 3. `RendezVous`

```java
@Entity
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RendezVous {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Temporal(TemporalType.TIMESTAMP)
    private Date date;
    private STATUS status;

    @ManyToOne
    private Patient patient;

    @ManyToOne
    private Medecin medecin;

    @OneToOne(mappedBy = "rendezVous")
    private Consultation consultation;
}
```

### 4. `Consultation`

```java
@Entity
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Consultation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Temporal(TemporalType.TIMESTAMP)
    private Date dateConsultation;
    private String rapport;

    @OneToOne
    private RendezVous rendezVous;
}
```

### 5. `Utilisateur` & `Role`

```java
@Entity
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Utilisateur {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    private String password;

    @ManyToMany(fetch = FetchType.EAGER)
    private List<Role> roles;
}
```

```java
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String roleName;
}
```

### 6. Enum `STATUS`

```java
public enum STATUS {
    EN_ATTENTE,
    CONFIRME,
    ANNULE,
    TERMINE
}
```

---

##  Configuration

### Fichier `application.properties`

```properties
spring.application.name=hospital_app
spring.datasource.url=jdbc:mysql://localhost:3306/hospital_db?useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MariaDBDialect
server.port=8080
```

---

##  Initialisation des Données

### Classe principale : `HospitalAppApplication`

```java
@SpringBootApplication
public class HospitalAppApplication implements CommandLineRunner {

    @Autowired private PatientRepository patientRepository;
    @Autowired private MedecinRepository medecinRepository;
    @Autowired private RendezVousRepository rendezVousRepository;
    @Autowired private ConsultationRepository consultationRepository;

    public static void main(String[] args) {
        SpringApplication.run(HospitalAppApplication.class, args);
    }

    @Override
    public void run(String... args) {
        patientRepository.save(new Patient(null, "messi", new Date(), true, 10));
        patientRepository.save(new Patient(null, "hafid", new Date(), false, 20));
        patientRepository.save(new Patient(null, "Karim", new Date(), true, 5));

        List<Patient> patients = patientRepository.findAll();
        patients.forEach(p -> System.out.println(p.getNom()));

        Patient patient = patients.get(0);
        patient.setScore(99);
        patientRepository.save(patient);

        Long idToDelete = patients.get(1).getId();
        patientRepository.deleteById(idToDelete);

        Medecin medecin = Medecin.builder()
                .nom("Dr. Salma")
                .specialite("Cardiologie")
                .build();
        medecin = medecinRepository.save(medecin);

        RendezVous rdv = RendezVous.builder()
                .date(new Date())
                .status(STATUS.EN_ATTENTE)
                .patient(patient)
                .medecin(medecin)
                .build();
        rdv = rendezVousRepository.save(rdv);

        Consultation consultation = Consultation.builder()
                .dateConsultation(new Date())
                .rapport("Consultation initiale : état stable.")
                .rendezVous(rdv)
                .build();
        consultationRepository.save(consultation);
    }
}
```

---

##  Test Utilisateur & Rôle (avec utilisateur `ADAM`)

```java
package com.fsm.hospital;

import com.fsm.hospital.entities.*;
import com.fsm.hospital.repositories.ConsultationRepository;
import com.fsm.hospital.repositories.MedecinRepository;
import com.fsm.hospital.repositories.RendezVousRepository;
import com.fsm.hospital.service.IHospitalService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import com.fsm.hospital.repositories.PatientRepository;

@SpringBootApplication
public class HospitalApplication {

	public static void main(String[] args) {
		SpringApplication.run(HospitalApplication.class, args);
	}
	@Bean
	CommandLineRunner start(IHospitalService hospitalService,PatientRepository patientRepository,
							MedecinRepository medecinRepository, RendezVousRepository rendezVousRepository,
							ConsultationRepository consultationRepository) {
		return args -> {
			Stream.of("Mohamed", "Hassan", "Najat")
					.forEach(name -> {
						Patient patient = new Patient();
						patient.setNom(name);
						patient.setDateNaissance(new Date());
						patient.setMalade(false);
						hospitalService.savePatient(patient);
					});
			Stream.of("Yassmine", "Amin", "Nour")
					.forEach(name -> {
						Medecin medecin = new Medecin();
						medecin.setNom(name);
						medecin.setEmail(name+"_medecin@emailcom");
						medecin.setSpecialite(Math.random()>0.5?"Cardio":"Dentiste");
						hospitalService.saveMedecin(medecin);
					});

			// Recherche d'un patient par ID (retourne null si non trouvé)
			Patient patient = patientRepository.findById(1L).orElse(null);

			// Recherche d'un patient par nom
			Patient patient1 = patientRepository.findByNom("Mohamed");

			// Recherche d'un médecin par nom
			Medecin medecin = medecinRepository.findByNom("Yassmine");

			// Création d'un nouveau rendez-vous
			RendezVous rendezVous = new RendezVous();

			// Configuration du rendez-vous
			rendezVous.setDate(new Date());
			rendezVous.setStatus(StatusRDV.PENDING);
			rendezVous.setMedecin(medecin);
			rendezVous.setPatient(patient);

			// Sauvegarde du rendez-vous
			RendezVous savedRDV = hospitalService.saveRDV(rendezVous);
			System.out.println(savedRDV.getId());

			// Récupération d'un rendez-vous existant
			RendezVous rendezVous1 = rendezVousRepository.findAll().get(0);

			// Création d'une nouvelle consultation
			Consultation consultation = new Consultation();

			// Configuration de la consultation
			consultation.setDateConsultation(new Date());
			consultation.setRendezVous(rendezVous1);
			consultation.setRapport("Rapport de la consultation...");
			hospitalService.saveConsultation(consultation);

		};
	}

}

```

---

##  Conclusion

Ce projet a permis de :

* Mettre en œuvre les annotations JPA pour modéliser des entités et leurs relations.
* Configurer l’accès à une base de données MySQL avec Hibernate.
* Utiliser `CommandLineRunner` pour initialiser les données au démarrage de l’application.
* Créer une base pour un futur système d’authentification basé sur des rôles.

L'application peut être enrichie à l’avenir par :

* L’ajout de services REST via Spring MVC.
* La mise en place de la sécurité avec Spring Security.
* L’intégration d’un frontend Angular/React ou mobile.
* Une documentation via Swagger.

---
