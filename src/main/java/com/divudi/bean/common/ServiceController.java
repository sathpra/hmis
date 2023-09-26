/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.common;

import com.divudi.data.BillType;
import com.divudi.data.DepartmentType;
import com.divudi.data.FeeType;
import com.divudi.data.SessionNumberType;
import com.divudi.data.dataStructure.ServiceFee;
import com.divudi.data.inward.InwardChargeType;
import com.divudi.entity.Category;
import com.divudi.entity.Department;
import com.divudi.entity.Fee;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.ItemFee;
import com.divudi.entity.Service;
import com.divudi.entity.clinical.ClinicalEntity;
import com.divudi.facade.CategoryFacade;
import com.divudi.facade.DepartmentFacade;
import com.divudi.facade.FeeFacade;
import com.divudi.facade.ItemFeeFacade;
import com.divudi.facade.ServiceCategoryFacade;
import com.divudi.facade.ServiceFacade;
import com.divudi.facade.ServiceSubCategoryFacade;
import com.divudi.facade.SpecialityFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;
import org.primefaces.model.file.UploadedFile;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class ServiceController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @Inject
    private ServiceSubCategoryController serviceSubCategoryController;
    @Inject
    CommonController commonController;
    @EJB
    private ServiceFacade ejbFacade;
    @EJB
    private SpecialityFacade specialityFacade;
    @Inject
    private BillBeanController billBean;
    @EJB
    private ServiceCategoryFacade serviceCategoryFacade;
    @EJB
    private ServiceSubCategoryFacade serviceSubCategoryFacade;
    @EJB
    private CategoryFacade categoryFacade;
    @EJB
    private FeeFacade feeFacade;
    @EJB
    private ItemFeeFacade itemFeeFacade;

    List<Service> selectedItems;
    List<Service> selectedRetiredItems;
    private Service current;
    Service currentInactiveService;
    private List<Service> items = null;
    private List<Service> filterItem;
    String selectText = "";
    String selectRetiredText = "";
    String bulkText = "";
    boolean billedAs;
    boolean reportedAs;
    @EJB
    private DepartmentFacade departmentFacade;
    List<Service> itemsToRemove;
    private UploadedFile file;
    private Institution institution;
    private Department department;
    private Category category;
    private InwardChargeType inwardChargeType;

    public List<Service> getItemsToRemove() {
        return itemsToRemove;
    }

    public void setItemsToRemove(List<Service> itemsToRemove) {
        this.itemsToRemove = itemsToRemove;
    }

    
    
    public void downloadAsExcel() {
        getItems();
        try {
            // Create a new Excel workbook
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Proecdures");

            // Create a header row
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("No");
            headerRow.createCell(1).setCellValue("Name");
            // Add more columns as needed

            // Populate the data rows
            int rowNum = 1;
            for (ClinicalEntity sym : items) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(rowNum);
                row.createCell(1).setCellValue(sym.getName());
            }

            // Set the response headers to initiate the download
            FacesContext context = FacesContext.getCurrentInstance();
            HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=\"procedures.xlsx\"");

            // Write the workbook to the response output stream
            workbook.write(response.getOutputStream());
            workbook.close();
            context.responseComplete();
        } catch (Exception e) {
            // Handle any exceptions
            e.printStackTrace();
        }
    }
    
    public void removeSelectedItems() {
        for (Service s : itemsToRemove) {
            s.setRetired(true);
            s.setRetireComments("Bulk Remove");
            s.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(s);
        }
        itemsToRemove = null;
        items = null;
    }

    public List<Service> getSelectedRetiredItems() {
        return selectedRetiredItems;
    }

    public void setSelectedRetiredItems(List<Service> selectedRetiredItems) {
        this.selectedRetiredItems = selectedRetiredItems;
    }

    public String getSelectRetiredText() {
        return selectRetiredText;
    }

    public void setSelectRetiredText(String selectRetiredText) {
        this.selectRetiredText = selectRetiredText;
    }

    public Service getCurrentInactiveService() {
        return currentInactiveService;
    }

    public void setCurrentInactiveService(Service currentInactiveService) {
        this.currentInactiveService = currentInactiveService;
    }

    public List<Department> getInstitutionDepatrments() {
        List<Department> d;
        //////// // System.out.println("gettin ins dep ");
        if (getCurrent().getInstitution() == null) {
            return new ArrayList<>();
        } else {
            String sql = "Select d From Department d where d.retired=false and d.institution.id=" + getCurrent().getInstitution().getId();
            d = getDepartmentFacade().findByJpql(sql);
        }

        return d;
    }

    public List<Service> completeService(String query) {
        List<Service> suggestions;
        String sql;
        if (query == null) {
            suggestions = new ArrayList<Service>();
        } else {
            sql = "select c from Service c where c.retired=false and (c.name) like '%" + query.toUpperCase() + "%' order by c.name";
            //////// // System.out.println(sql);
            suggestions = getFacade().findByJpql(sql);
        }
        return suggestions;
    }

    public List<Service> getSelectedItems() {
        if (selectText.trim().equals("")) {
            selectedItems = getFacade().findByJpql("select c from Service c where c.retired=false order by c.name");
        } else {
            selectedItems = getFacade().findByJpql("select c from Service c where c.retired=false and (c.name) like '%" + getSelectText().toUpperCase() + "%' order by c.name");
        }
        return selectedItems;
    }

    public List<Service> getRetiredSelectedItems() {
        if (selectRetiredText.trim().equals("")) {
            selectedRetiredItems = getFacade().findByJpql("select c from Service c where c.retired=true order by c.name");
        } else {
            selectedRetiredItems = getFacade().findByJpql("select c from Service c where c.retired=true and (c.name) like '%" + getSelectRetiredText().toUpperCase() + "%' order by c.name");
        }
        return selectedRetiredItems;
    }

    public boolean isBilledAs() {
        return billedAs;
    }

    public void setBilledAs(boolean billedAs) {
        this.billedAs = billedAs;
    }

    public boolean isReportedAs() {
        return reportedAs;
    }

    public void setReportedAs(boolean reportedAs) {
        this.reportedAs = reportedAs;
    }

    public String getBulkText() {

        return bulkText;
    }

    public void setBulkText(String bulkText) {
        this.bulkText = bulkText;
    }

    public List<Service> completeItem(String qry) {
        List<Service> completeItems = getFacade().findByJpql("select c from Item c "
                + " where ( type(c) = Service or type(c) = Packege ) "
                + " and c.retired=false and (c.name) like '%" + qry.toUpperCase() + "%'"
                + "  order by c.name");
        return completeItems;
    }

    public void prepareAdd() {
        current = new Service();
    }

    public void bulkUpload() {
        List<String> lstLines = Arrays.asList(getBulkText().split("\\r?\\n"));
        for (String s : lstLines) {
            List<String> w = Arrays.asList(s.split(","));
            try {
                String code = w.get(0);
                String ix = w.get(1);
                String ic = w.get(2);
                String f = w.get(4);
                //////// // System.out.println(code + " " + ix + " " + ic + " " + f);

                Service tix = new Service();
                tix.setCode(code);
                tix.setName(ix);
                tix.setDepartment(null);

            } catch (Exception e) {
            }

        }
    }

    public void setSelectedItems(List<Service> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public String getSelectText() {
        return selectText;
    }

    public void recreateModel() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        items = null;
        filterItem = null;

        commonController.printReportDetails(fromDate, toDate, startTime, "Reports/Check Entered Data/Service/Service list search(/faces/dataAdmin/opd_service_department_list.xhtml)");
    }

    private boolean errorCheck() {
        if (getCurrent().isUserChangable() && getCurrent().isDiscountAllowed() == true) {
            UtilityController.addErrorMessage("Cant tick both User can Change & Discount Allowed");
            return true;
        }
        return false;
    }

    /**
     * Generates a shortened code for a given name.
     *
     * @param name The full name of the service, item, or product.
     * @return A shortened code for the name.
     */
    private String generateShortCode(String name) {
        // Initialize the code as an empty string.
        String code = "";

        // Split the name into words using space as the delimiter.
        String[] words = name.split(" ");

        // If there's only one word, take the first three letters as the code.
        if (words.length == 1 && words[0].length() >= 3) {
            code = words[0].substring(0, 3).toLowerCase();
        } else {
            // If there are multiple words, take the first letter of each word as the code.
            for (String word : words) {
                if (!word.isEmpty()) {
                    code += word.charAt(0);
                }
            }
            // Make the code lowercase for simplicity.
            code = code.toLowerCase();
        }

        return code;
    }

    public void saveSelected() {
        if (getCurrent().getDepartment() == null) {
            UtilityController.addErrorMessage("Please Select Department");
            return;
        }
        if (getCurrent().getInwardChargeType() == null) {
            UtilityController.addErrorMessage("Please Select Inward Charge type");
            return;
        }
        if (getCurrent().getName() == null || getCurrent().getName().isEmpty()) {
            UtilityController.addErrorMessage("Please Enter a name");
        } else {
            if (getCurrent().getFullName() == null) {
                getCurrent().setFullName(getCurrent().getName());
            }
            if (getCurrent().getPrintName() == null) {
                getCurrent().setPrintName(getCurrent().getName());
            }
            if (getCurrent().getCode() == null || getCurrent().getCode().isEmpty()) {
                getCurrent().setCode(generateShortCode(getCurrent().getName()));
            }
        }

//        if (errorCheck()) {
//            return;
//        }
//        if (getServiceSubCategoryController().getParentCategory() != null) {
//            getCurrent().setCategory(getServiceSubCategoryController().getParentCategory());
//        }
        ////// // System.out.println("getCurrent().getId() = " + getCurrent());
        ////// // System.out.println("getCurrent().getId() = " + getCurrent().getId());
        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            //////// // System.out.println("1");
            if (billedAs == false) {
                //////// // System.out.println("2");
                getCurrent().setBilledAs(getCurrent());

            }
            if (reportedAs == false) {
                //////// // System.out.println("3");
                getCurrent().setReportedAs(getCurrent());
            }
            getFacade().edit(getCurrent());
            UtilityController.addSuccessMessage("Saved Old Successfully");
        } else {
            //////// // System.out.println("4");
            getCurrent().setCreatedAt(new Date());
            getCurrent().setCreater(getSessionController().getLoggedUser());
            getFacade().create(getCurrent());
            if (billedAs == false) {
                //////// // System.out.println("5");
                getCurrent().setBilledAs(getCurrent());
            }
            if (reportedAs == false) {
                //////// // System.out.println("6");
                getCurrent().setReportedAs(getCurrent());
            }
            getFacade().edit(getCurrent());
            UtilityController.addSuccessMessage("Saved Successfully");
        }
        recreateModel();
        getItems();
    }

    public void setSelectText(String selectText) {
        recreateModel();
        this.selectText = selectText;
    }

    public ServiceFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(ServiceFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public ServiceController() {
    }

    public Service getCurrent() {
        if (current == null) {
            current = new Service();
        }
        return current;
    }

    public void setCurrent(Service current) {
        this.current = current;
        if (current != null) {
            if (current.getBilledAs() == current) {
                billedAs = false;
            } else {
                billedAs = true;
            }
            if (current.getReportedAs() == current) {
                reportedAs = false;
            } else {
                reportedAs = true;
            }
        }
    }

    public void delete() {

        for (ItemFee it : getFees(current)) {
            it.setRetired(true);
            it.setRetiredAt(new Date());
            it.setRetirer(getSessionController().getLoggedUser());
            getItemFeeFacade().edit(it);
        }

        if (current != null) {
            current.setRetired(true);
            current.setRetiredAt(new Date());
            current.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(current);
            UtilityController.addSuccessMessage("Deleted Successfull");
        } else {
            UtilityController.addSuccessMessage("Nothing to Delete");
        }
        recreateModel();
        getSelectedItems();

    }

    public void activateService() {

        for (ItemFee it : getFees(currentInactiveService)) {
            it.setRetired(true);
            it.setRetiredAt(new Date());
            it.setRetirer(getSessionController().getLoggedUser());
            getItemFeeFacade().edit(it);
        }

        if (currentInactiveService != null) {
            currentInactiveService.setRetired(false);
            currentInactiveService.setRetiredAt(new Date());
            currentInactiveService.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(currentInactiveService);
            UtilityController.addSuccessMessage("Deleted Successfull");
        } else {
            UtilityController.addSuccessMessage("Nothing to Delete");
        }
        recreateModel();
        getSelectedRetiredItems();

    }

    private ServiceFacade getFacade() {
        return ejbFacade;
    }

    public List<ItemFee> getItemFee() {
        List<ItemFee> temp;
        HashMap hash = new HashMap();
        String sql = "select c from ItemFee c where c.retired = false and type(c.item) = :ser order by c.item.name";
        hash.put("ser", Service.class);
        temp = getItemFeeFacade().findByJpql(sql, hash, TemporalType.TIMESTAMP);

        if (temp == null) {
            return new ArrayList<ItemFee>();
        }

        return temp;
    }

    public List<ServiceFee> getServiceFee() {

        List<ServiceFee> temp = new ArrayList<ServiceFee>();

        for (Service s : getItem()) {
            ServiceFee si = new ServiceFee();
            si.setService(s);

            String sql = "select c from ItemFee c where c.retired = false and c.item.id =" + s.getId();

            si.setItemFees(getItemFeeFacade().findByJpql(sql));

            temp.add(si);
        }

        return temp;
    }

    List<Service> deletedServices;
    List<Service> deletingServices;

    public List<Service> getDeletedServices() {
        return deletedServices;
    }

    public void setDeletedServices(List<Service> deletedServices) {
        this.deletedServices = deletedServices;
    }

    public List<Service> getDeletingServices() {
        return deletingServices;
    }

    public void setDeletingServices(List<Service> deletingServices) {
        this.deletingServices = deletingServices;
    }

    public void listDeletedServices() {
        String sql = "select c from Service c where c.retired=true order by c.category.name,c.department.name";
        deletedServices = getFacade().findByJpql(sql);
        if (deletedServices == null) {
            deletedServices = new ArrayList<>();
        }
    }

    public void undeleteSelectedServices() {
        for (Service s : deletingServices) {
            s.setRetired(false);
            s.setRetiredAt(null);
            s.setRetirer(null);
            getFacade().edit(s);
            ////// // System.out.println("undeleted = " + s);
        }
        deletingServices = null;
        listDeletedServices();
    }

    public List<Service> getItems() {
        if (items == null) {
            fillItems();
        }
        return items;
    }

    /**
     * Handles the uploaded Excel file and extracts data from columns A and B.
     *
     * @return Map with name (from column A) as key and price (from column B) as
     * value.
     */
    public void uploadOpdItemNamesAndFee() {
        Map<String, Double> resultMap = new HashMap<>();

        if (file != null) {
            try ( InputStream input = file.getInputStream()) {
                Workbook workbook = new XSSFWorkbook(input);
                Sheet sheet = workbook.getSheetAt(0);
                for (Row row : sheet) {
                    if (row.getRowNum() == 0) { // skip the title row
                        continue;
                    }

                    Cell nameCell = row.getCell(0);
                    Cell priceCell = row.getCell(1);

                    if (nameCell != null && priceCell != null) {
                        String name = nameCell.getStringCellValue();
                        Double price = priceCell.getNumericCellValue();
                        resultMap.put(name, price);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        addItemsAndFees(resultMap);
    }

    public void addItemsAndFees(Map<String, Double> resultMap) {
        Department dept = getDepartment();
        Institution ins = getInstitution();
        Category cat = getCategory();
        InwardChargeType ic = getInwardChargeType();

        for (Map.Entry<String, Double> entry : resultMap.entrySet()) {
            String name = entry.getKey();
            Double fee = entry.getValue();
            Item item = addItem(name, fee, dept, ins, cat, ic);
            addFee(item, fee);
        }
    }

    private Service addItem(String name, Double fee, Department dept, Institution ins, Category cat, InwardChargeType ic) {
        Service i = new Service();
        i.setName(name);
        i.setFullName(name);
        i.setPrintName(name);
        i.setInwardChargeType(ic);
        i.setCreatedAt(new Date());
        i.setCreater(sessionController.getLoggedUser());
        i.setCategory(cat);
        i.setDblValue(fee);
        i.setDepartment(dept);
        i.setInstitution(ins);
        i.setDepartmentType(DepartmentType.Opd);
        i.setDiscountAllowed(true);
        i.setForBillType(BillType.OpdBill);
        i.setUserChangable(true);
        i.setTotalForForeigner(fee);
        i.setTotal(fee);
        i.setTotalFee(0);
        i.setTotalFfee(fee);
        i.setRequestForQuentity(true);
        ejbFacade.create(i);
        return i;
    }

    private void addFee(Item item, Double fee) {
        ItemFee f = new ItemFee();
        f.setItem(item);
        f.setFee(fee);
        f.setFeeType(FeeType.Service);
        f.setCode("hospital_fee");
        f.setCreatedAt(new Date());
        f.setCreater(sessionController.getLoggedUser());
        f.setDepartment(item.getDepartment());
        f.setInstitution(item.getInstitution());
        f.setDiscountAllowed(true);
        f.setFfee(fee);
        f.setHospitalFee(fee);
        f.setHospitalFfee(fee);
        f.setName("Hospital Fee");
        itemFeeFacade.create(f);
    }

    public void fillItems() {
        String sql = "select c "
                + " from Service c "
                + " where c.retired=false "
                + " order by c.category.name,c.department.name";
        items = getFacade().findByJpql(sql);
        for (Service i : items) {
            List<ItemFee> tmp = getFees(i);
            for (ItemFee itf : tmp) {
                i.setItemFee(itf);
            }
        }
        if (items == null) {
            items = new ArrayList<Service>();
        }
    }

    public List<Service> getItem() {
        String sql;
        if (selectText.isEmpty()) {
            sql = "select c from Service c where c.retired=false order by c.category.name,c.name";
        } else {
            sql = "select c from Service c where c.retired=false and (c.name) like '%" + selectText.toUpperCase() + "%' order by c.category.name,c.name";
        }
        //////// // System.out.println(sql);
        items = getFacade().findByJpql(sql);

        if (items == null) {
            items = new ArrayList<Service>();
        }
        return items;
    }

    public List<Service> getServiceDep() {
        if (items == null) {
            String sql;
            sql = "select c from Service c where c.retired=false order by c.category.name,c.name";

            //////// // System.out.println(sql);
            items = getFacade().findByJpql(sql);

            for (Service i : items) {

                List<ItemFee> tmp = getFees(i);
                for (ItemFee itf : tmp) {
                    i.setItemFee(itf);
                    if (itf.getFeeType() == FeeType.OwnInstitution) {
                        i.setHospitalFee(i.getHospitalFee() + itf.getFee());
                        i.setHospitalFfee(i.getHospitalFfee() + itf.getFfee());
                    } else if (itf.getFeeType() == FeeType.Staff) {
                        i.setProfessionalFee(i.getProfessionalFee() + itf.getFee());
                        i.setProfessionalFfee(i.getProfessionalFfee() + itf.getFfee());
                    }
                }
            }

            if (items == null) {
                items = new ArrayList<Service>();
            }

        }
        return items;
    }

    public List<ItemFee> getFees(Item i) {
        String sql = "Select f From ItemFee f where f.retired=false and f.item.id=" + i.getId();

        return getItemFeeFacade().findByJpql(sql);
    }

    public SpecialityFacade getSpecialityFacade() {
        return specialityFacade;
    }

    public void setSpecialityFacade(SpecialityFacade specialityFacade) {
        this.specialityFacade = specialityFacade;
    }

    public ServiceSubCategoryController getServiceSubCategoryController() {
        return serviceSubCategoryController;
    }

    public void setServiceSubCategoryController(ServiceSubCategoryController serviceSubCategoryController) {
        this.serviceSubCategoryController = serviceSubCategoryController;
    }

    public ServiceCategoryFacade getServiceCategoryFacade() {
        return serviceCategoryFacade;
    }

    public void setServiceCategoryFacade(ServiceCategoryFacade serviceCategoryFacade) {
        this.serviceCategoryFacade = serviceCategoryFacade;
    }

    public ServiceSubCategoryFacade getServiceSubCategoryFacade() {
        return serviceSubCategoryFacade;
    }

    public void setServiceSubCategoryFacade(ServiceSubCategoryFacade serviceSubCategoryFacade) {
        this.serviceSubCategoryFacade = serviceSubCategoryFacade;
    }

    public CategoryFacade getCategoryFacade() {
        return categoryFacade;
    }

    public void setCategoryFacade(CategoryFacade categoryFacade) {
        this.categoryFacade = categoryFacade;
    }

    public BillBeanController getBillBean() {
        return billBean;
    }

    public void setBillBean(BillBeanController billBean) {
        this.billBean = billBean;
    }

    public SessionNumberType[] getSessionNumberType() {
        return SessionNumberType.values();
    }

    public ItemFeeFacade getItemFeeFacade() {
        return itemFeeFacade;
    }

    // Getter and setter for file
    public UploadedFile getFile() {
        return file;
    }

    public void setFile(UploadedFile file) {
        this.file = file;
    }

    public void setItemFeeFacade(ItemFeeFacade itemFeeFacade) {
        this.itemFeeFacade = itemFeeFacade;
    }

    public DepartmentFacade getDepartmentFacade() {
        return departmentFacade;
    }

    public void setDepartmentFacade(DepartmentFacade departmentFacade) {
        this.departmentFacade = departmentFacade;
    }

    public List<Service> getFilterItem() {
        return filterItem;
    }

    public void setFilterItem(List<Service> filterItem) {
        this.filterItem = filterItem;
    }

    public FeeFacade getFeeFacade() {
        return feeFacade;
    }

    public void setFeeFacade(FeeFacade feeFacade) {
        this.feeFacade = feeFacade;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public InwardChargeType getInwardChargeType() {
        return inwardChargeType;
    }

    public void setInwardChargeType(InwardChargeType inwardChargeType) {
        this.inwardChargeType = inwardChargeType;
    }

    /**
     *
     */
    @FacesConverter(forClass = Service.class)
    public static class ServiceControllerConverter implements Converter {

        public ServiceControllerConverter() {
        }

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            ServiceController controller = (ServiceController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "serviceController");
            return controller.getEjbFacade().find(getKey(value));
        }

        java.lang.Long getKey(String value) {
            java.lang.Long key;
            key = Long.valueOf(value);
            return key;
        }

        String getStringKey(java.lang.Long value) {
            StringBuilder sb = new StringBuilder();
            sb.append(value);
            return sb.toString();
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof Service) {
                Service o = (Service) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + ServiceController.class.getName());
            }
        }
    }

    public CommonController getCommonController() {
        return commonController;
    }

    public void setCommonController(CommonController commonController) {
        this.commonController = commonController;
    }

}
