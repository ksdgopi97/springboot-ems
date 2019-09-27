package com.pepperkick.ems.route;

import com.pepperkick.ems.entity.Designation;
import com.pepperkick.ems.entity.Employee;
import com.pepperkick.ems.repository.DesignationRepository;
import com.pepperkick.ems.repository.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping(value = "/employee")
public class EmployeeRoute {
    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DesignationRepository designationRepository;
    private Designation mainDesignation = null;
    private Designation leastDesignation = null;

    private final Logger logger = LoggerFactory.getLogger(EmployeeRepository.class);

    @PostConstruct
    public void init() {
        List<Designation> designations = designationRepository.findByLevel(1);
        if (designations.size() == 1) mainDesignation = designations.get(0);
        Designation designation = designationRepository.findByTitle("Intern");
        if (designations.size() == 1) leastDesignation = designation;
    }

    @RequestMapping(method= RequestMethod.GET, produces = "application/json")
    public List<Employee> get() {
        Iterable<Employee> employee = employeeRepository.findAll();
        List<Employee> employees = new ArrayList<>();

        for (Employee e : employee) {
            employees.add(e);
        }

        return employees;
    }

    @RequestMapping(method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity post(@RequestBody Map<String, Object> payload) {
        String name = (String) payload.get("name");
        String jobTitle = (String) payload.get("jobTitle");
        int managerId = Integer.parseInt("" + payload.get("managerId"));

        if (name.compareTo("") == 0) {
            return new ResponseEntity<>("Name cannot be empty", HttpStatus.NOT_FOUND);
        }

        Designation designation = designationRepository.findByTitle(jobTitle);
        if (designation == null){
            return new ResponseEntity<>("Designation not found", HttpStatus.NOT_FOUND);
        } else if (designation.getLevel() == 1) {
            if (mainDesignation == null)
                return new ResponseEntity<>("Unable to verify if a director is present at this time", HttpStatus.METHOD_NOT_ALLOWED);

            List<Employee> employees = employeeRepository.findEmployeeByDesignation(mainDesignation);

            if (employees.size() != 0)
                return new ResponseEntity<>("Only one director can be present at one time", HttpStatus.METHOD_NOT_ALLOWED);
        }

        Employee manager = employeeRepository.findById(managerId);
        if (manager == null) {
            return new ResponseEntity<>("Manager not found", HttpStatus.NOT_FOUND);
        } else if (manager.getDesignation().getLevel() >= designation.getLevel()) {
            return new ResponseEntity<>("Manager cannot be designated lower or equal level to subordinate", HttpStatus.METHOD_NOT_ALLOWED);
        }

        Employee newEmployee = new Employee();
        newEmployee.setName(name);
        newEmployee.setManager(manager);
        newEmployee.setDesignation(designation);
        employeeRepository.save(newEmployee);

        return new ResponseEntity(HttpStatus.OK);
    }

    @RequestMapping(value= "/{id}", method= RequestMethod.GET, produces = "application/json")
    public ResponseEntity get(@PathVariable int id) {
        if (id < 0)
            return new ResponseEntity(HttpStatus.NOT_ACCEPTABLE);

        Employee employee = employeeRepository.findById(id);

        if (employee != null)
            return new ResponseEntity<Object>(employee, HttpStatus.OK);

        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(value= "/{id}", method= RequestMethod.PUT, produces = "application/json", consumes = "application/json")
    public ResponseEntity put(@PathVariable int id, @RequestBody Map<String, Object> payload) {
        String name = null;
        String jobTitle = null;
        int managerId = -1;
        boolean replace = false;

        if (payload.get("name") != null)
            name = (String) payload.get("name");
        if (payload.get("jobTitle") != null)
            jobTitle = (String) payload.get("jobTitle");
        if (payload.get("managerId") != null)
            managerId = Integer.parseInt("" + payload.get("managerId"));
        if (payload.get("replace") !=  null)
            replace = Boolean.parseBoolean("" + payload.get("replace"));

        Employee employee = employeeRepository.findById(id);

        if (employee == null)
            return new ResponseEntity(HttpStatus.NOT_FOUND);

        if (replace) {
            if (name == null || jobTitle == null)
                return new ResponseEntity<>("Required parameters missing", HttpStatus.NOT_ACCEPTABLE);

            Designation designation = designationRepository.findByTitle(jobTitle);
            Employee oldEmployee = employee;

            if (designation == mainDesignation && managerId != -1)
                return new ResponseEntity<>("Director cannot have a manager", HttpStatus.METHOD_NOT_ALLOWED);

            Employee manager = employeeRepository.findById(managerId);

            if (manager == null)
                return new ResponseEntity<>("Manager not found", HttpStatus.NOT_FOUND);

            if (manager.getDesignation().getLevel() >= designation.getLevel())
                return new ResponseEntity<>("New manager designation level cannot be lower than current employee", HttpStatus.METHOD_NOT_ALLOWED);

            employee = new Employee();
            employee.setName(name);
            employee.setDesignation(designation);
            employee.setManager(manager);
            employeeRepository.save(employee);

            for (Employee sub : oldEmployee.getSubordinates()) {
                sub.setManager(employee);
                employeeRepository.save(sub);
            }

            employeeRepository.delete(oldEmployee);
        } else {
            if (name != null) employee.setName(name);

            if (jobTitle != null) {
                Designation designation = designationRepository.findByTitle(jobTitle);

                if (designation == null)
                    return new ResponseEntity<>("Could not find designation", HttpStatus.NOT_FOUND);

                if (designation == mainDesignation)
                    return new ResponseEntity<>("Cannot change designation of director", HttpStatus.METHOD_NOT_ALLOWED);

                if (employee.getSubordinates().size() > 0) {
                    Designation highest = employee.getDesignation();

                    for (Employee sub : employee.getSubordinates()) {
                        if (sub.getDesignation().getLevel() > highest.getLevel())
                            highest = sub.getDesignation();
                    }

                    if (designation.getLevel() >= highest.getLevel())
                        return new ResponseEntity<>("Employee designation cannot be lower than its subordinates", HttpStatus.METHOD_NOT_ALLOWED);
                }

                employee.setDesignation(designation);
            }

            if (managerId != -1) {
                Employee manager = employeeRepository.findById(managerId);

                if (manager == null)
                    return new ResponseEntity<>("Manager not found", HttpStatus.NOT_FOUND);

                if (manager.getDesignation().getLevel() >= employee.getDesignation().getLevel())
                    return new ResponseEntity<>("New manager designation level cannot be lower than current employee", HttpStatus.METHOD_NOT_ALLOWED);

                employee.setManager(manager);
            }

            employeeRepository.save(employee);
        }

        return new ResponseEntity(HttpStatus.OK);
    }

    @RequestMapping(value= "/{id}", method = RequestMethod.DELETE, produces = "application/text")
    public ResponseEntity delete(@PathVariable int id) {
        if (id < 0)
            return new ResponseEntity(HttpStatus.NOT_ACCEPTABLE);

        Employee employee = employeeRepository.findById(id);;

        if (employee == null)
            return new ResponseEntity(HttpStatus.NOT_FOUND);

        if (employee.getDesignation().getLevel() == 1) {
            if (!employee.getSubordinates().isEmpty())
                return new ResponseEntity(HttpStatus.METHOD_NOT_ALLOWED);
        }

        if (!employee.getSubordinates().isEmpty()) {
            Employee manager = employee.getManager();
            employee.getSubordinates().forEach(object -> {
                object.setManager(manager);
                employeeRepository.save(object);
            });
        }

        employeeRepository.delete(employee);

        return new ResponseEntity(HttpStatus.OK);
    }
}
