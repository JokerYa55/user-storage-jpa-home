package DAO;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import org.jboss.logging.Logger;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.persistence.EntityManager;
import keycloak.bean.UserAttribute;
import keycloak.bean.UserEntity;
import keycloak.bean.UserRequiredAction;
import keycloak.bean.UsersSmsMessages;
import static keycloak.storage.util.hashUtil.encodeToHex;
import static keycloak.storage.util.hashUtil.genSalt;
//import static keycloak.storage.util.hashUtil.sha1ToString;
//import static keycloak.storage.util.hashUtil.encodeToHex;
import static keycloak.storage.util.hashUtil.sha1;

// <!-- <property name="hibernate.show_sql" value="true"/> -->
/**
 * Класс для представления пользователя внутри Keycloak
 *
 * @version 1
 * @author Vasiliy Andritsov
 *
 */
public class UserAdapter extends AbstractUserAdapterFederatedStorage {
    
    private static final Logger log = Logger.getLogger(UserAdapter.class);
    protected UserEntity entity;
    protected String keycloakId;
    protected EntityManager em;

    /**
     *
     * @param session
     * @param realm
     * @param model
     * @param entity
     * @param em
     */
    public UserAdapter(KeycloakSession session, RealmModel realm, ComponentModel model, UserEntity entity, EntityManager em) {
        super(session, realm, model);
        log.debug("UserAdapter CONSTRUCTOR => entity = " + entity);
        this.entity = entity;
        // внутренний ID
        keycloakId = StorageId.keycloakId(model, entity.getId().toString());
        log.debug("keycloakId => " + keycloakId);
        this.em = em;
    }

    /**
     * Возвращает строку записаную в БД в поле password
     *
     * @return - возвращает пароль сохраненный в БД
     */
    public String getPassword() {
        log.debug("getPassword => " + entity.getPassword());
        return entity.getPassword();
    }

    /**
     * Записывает hash пароля и незашифрованный пароль в БД
     *
     * @param password - пароль пользователя в незашифрованом виде
     */
    public void setPassword(String password) {
        log.debug("UserAdapter  setPassword => " + password);
        String salt = genSalt();
        //encodeToHex(UUID.randomUUID().toString().getBytes());
        log.debug("salt => " + password);
        entity.setPassword(encodeToHex(sha1(password + salt)));
        //entity.setPassword(sha1ToString(password + salt));
        log.debug("password => " + entity.getPassword());
        entity.setHash_type("sha1");
        entity.setSalt(salt);
        //entity.setPassword_not_hash(password);
    }

    /**
     *
     * @param hash
     */
    public void setHash(String hash) {
        log.info("setHash => " + hash);
        entity.setHash(hash);
        entity.setDescription("<ELK>");
    }

    /**
     * Получает значение для Salt из БД
     *
     * @return
     */
    public String getSalt() {
        return entity.getSalt();
    }

    /**
     * Устанавливает значение для Salt
     *
     * @param salt
     */
    public void setSalt(String salt) {
        entity.setSalt(salt);
    }

    /**
     *
     * @return
     */
    public String getThirdName() {
        return entity.getThirdName();
    }

    /**
     *
     * @param thirdName
     */
    public void setThirdName(String thirdName) {
        entity.setThirdName(thirdName);
    }

    /**
     *
     * @return
     */
    public Integer getUser_region() {
        return entity.getUser_region();
    }

    /**
     *
     * @param region
     */
    public void setUserRegion(Integer region) {
        entity.setUser_region(region);
    }

    /**
     *
     * @param verified
     */
//    public Integer getUser_gender() {
//        return entity.getUser_gender();
//    }
    /**
     *
     * @param gender
     */
//    public void setUser_gender(Integer gender) {
//        entity.setUser_gender(gender);
//    }
    @Override
    public void setCreatedTimestamp(Long timestamp) {
        entity.setCreate_date(new Date(timestamp));
        //super.setCreatedTimestamp(timestamp); //To change body of generated methods, choose Tools | Templates.
    }
    
    @Override
    public Long getCreatedTimestamp() {
        //return super.getCreatedTimestamp(); //To change body of generated methods, choose Tools | Templates.
        return entity.getCreate_date().getTime();
    }
    
    @Override
    public void setEnabled(boolean enabled) {
        //super.setEnabled(enabled); //To change body of generated methods, choose Tools | Templates.
        entity.setEnabled(enabled);
    }
    
    @Override
    public boolean isEnabled() {
        //return super.isEnabled(); //To change body of generated methods, choose Tools | Templates.
        return entity.isEnabled();
    }

    /**
     * Возвращает строку с данными из поля username БД
     *
     * @return
     */
    @Override
    public String getUsername() {
        return entity.getUsername();
    }

    /**
     *
     * @param username
     */
    @Override
    public void setUsername(String username) {
        entity.setUsername(username.toLowerCase());
        
    }

    /**
     *
     * @param email
     */
    @Override
    public void setEmail(String email) {
        entity.setEmail(email);
    }

    /**
     *
     * @return
     */
    @Override
    public String getEmail() {
        return entity.getEmail();
    }

    /**
     *
     * @return
     */
    @Override
    public String getId() {
        return keycloakId;
    }
    
    @Override
    public void setLastName(String lastName) {
        entity.setLastName(lastName);
    }
    
    @Override
    public String getLastName() {
        return entity.getLastName();
    }
    
    @Override
    public void setFirstName(String firstName) {
        entity.setFirstName(firstName);
    }
    
    @Override
    public String getFirstName() {
        return entity.getFirstName();
    }

    /**
     *
     * @param name
     * @param value
     */
    @Override
    public void setSingleAttribute(String name, String value) {
        log.debug("setSingleAttribute => " + name + " : " + value);
        Pattern p = Pattern.compile("^id_app_[0-9]+$");
        Matcher m = p.matcher(name);
        if (name.equals("phone")) {
            entity.setPhone(value);
        } else if (name.equals("password")) {
            entity.setHash(value);
        } else if (m.matches()) {
            UserAttribute attr = new UserAttribute();
            attr.setName(name);
            attr.setValue(value);
            attr.setUserId(entity);
            entity.addUserAttribute(attr);
            
        } else {
            super.setSingleAttribute(name, value);
        }
    }

    /**
     *
     * @param name
     */
    @Override
    public void removeAttribute(String name) {
        log.debug("***** REMOVE ATTRUBUTE => " + name + "********");
        
        Pattern p = Pattern.compile("^id_app_[0-9]+$");
        Matcher m = p.matcher(name);
        
        if (m.matches()) {
            
            UserAttribute temp = null;
            for (UserAttribute t : entity.getUserAttributeCollection()) {
                if ((t.getName().equals(name)) && (!t.getName().equals("id_app_1"))) {
                    temp = t;
                }
            }
            if (temp != null) {
                log.debug("len => " + entity.getUserAttributeCollection().size());
                entity.getUserAttributeCollection().remove(temp);
                log.debug("len => " + entity.getUserAttributeCollection().size());
            } else {
                log.debug("ATTR NotFound");
            }
            
        } else {
            switch (name) {
                /*case "phone":
                    entity.setPhone(null);
                    break;
                case "salt":
                    entity.setSalt(null);
                    break;
                case "hash_type":
                    entity.setHesh_type(null);
                    break;
                case "region":
                    entity.setUser_region(null);
                    break;
                case "thirdName":
                    entity.setThirdName(null);
                    break;*/
                case "description":
                    entity.setDescription(null);
                    break;
                default:
                    super.removeAttribute(name);
                    break;
            }
        }
    }

    /**
     * Обновляет аттрибуты из интерфейса Keycloak
     *
     * @param name имя аттрибута
     * @param values Значения аттрибута (берется из локальной базы + из внешней
     * базы в случае федерации)
     */
    @Override
    public void setAttribute(String name, List<String> values) {
        log.debug("******* setAttribute => " + name + " : " + values.get(0) + " ******");
        UserAttribute attrib;
        //Collection<UserAttribute> attrList = entity.getUserAttributeCollection();
        if ((values.get(0) != null) && (values.get(0).length() > 0)) {
            Pattern p = Pattern.compile("^id_app_[0-9]+$");;
            Matcher m = p.matcher(name);
            if (m.matches()) {
                // Вставляем настраиваемые параметры                       
                attrib = new UserAttribute(name, values.get(0), entity, true);
                entity.addUserAttribute(attrib);
            } else {
                switch (name) {
                    case "phone":
                        entity.setPhone(values.get(0));
                        break;
                    case "hash":
                        entity.setHash(values.get(0));
                        break;
                    case "salt":
                        entity.setSalt(values.get(0));
                        break;
                    case "hash_type":
                        entity.setHash_type(values.get(0));
                        break;
                    case "thirdName":
                        entity.setThirdName(values.get(0));
                        break;
                    case "firstName":
                        entity.setFirstName(values.get(0));
                        break;
                    case "lastName":
                        entity.setLastName(values.get(0));
                        break;
                    case "region":
                        entity.setUser_region(new Integer(values.get(0)));
                        break;
                    case "description":
                        entity.setDescription(values.get(0));
                        break;
                    case "user_status":
                        entity.setUser_status(new Integer(values.get(0)));
                        break;
                    case "EMAIL_VERIFIED":
                        entity.setEmail_verified(Boolean.parseBoolean(values.get(0)));
                        break;
                    default:
                        super.setAttribute(name, values);
                        break;
                }
            }
            
        }
    }

    /**
     *
     * @param name
     * @return
     */
    @Override
    public String getFirstAttribute(String name) {
        log.debug("getFirstAttribute => " + name);
        switch (name) {
            case "phone":
                return entity.getPhone();
            case "region":
                return entity.getUser_region().toString();
            case "salt":
                return entity.getSalt();
            case "hash_type":
                return entity.getHash_type();
            case "thirdName":
                return entity.getThirdName();
            case "user_status":
                return entity.getUser_status().toString();
            
            default:
                return super.getFirstAttribute(name);
        }
    }
    
    @Override
    public void setEmailVerified(boolean verified) {
        entity.setEmail_verified(verified);
    }
    
    @Override
    public boolean isEmailVerified() {
        return entity.isEmail_verified();
    }

    /**
     * Метод позволяет добавлять аттрибуты из внешней базы в интерфейс Keycloak
     *
     * @return
     */
    @Override
    public Map<String, List<String>> getAttributes() {
        log.debug("getAttributes");
        Map<String, List<String>> attrs = super.getAttributes();
        
        MultivaluedHashMap<String, String> all = new MultivaluedHashMap<>();
        all.putAll(attrs);
        // Добавляем доп. аттрибуты в Keycloak
        log.info("************ Add user attibutes **************");
        
        if ((entity.getPhone() != null) && (entity.getPhone().length() > 0)) {
            // log.info("Add phone => " + entity.getPhone());
            all.add("phone", entity.getPhone());
        } else {
            all.add("phone", "");
        }
        
        if ((entity.getHash_type() != null) && (entity.getHash_type().length() > 0)) {
            //log.info("Add hash_type");
            all.add("hash_type", entity.getHash_type());
        } else {
            all.add("hash_type", "");
        }
        
        if ((entity.getThirdName() != null) && (entity.getThirdName().length() > 0)) {
            all.add("thirdName", entity.getThirdName());
        } else {
            all.add("thirdName", "");
        }
        
        if (entity.getUser_region() != null) {
            all.add("region", entity.getUser_region().toString());
        } else {
            all.add("region", "");
        }
        
        if (entity.getDescription() != null) {
            all.add("description", entity.getDescription());
        } else {
            all.add("description", "");
        }
        
        if (entity.isEmail_verified()) {
            all.add("EMAIL_VERIFIED", "true");
        } else {
            all.add("EMAIL_VERIFIED", "false");
        }
        
        Collection<UserAttribute> attrList = entity.getUserAttributeCollection();
        if (attrList != null) {
            attrList.forEach((t) -> {
                if (t.isVisible_flag()) {
                    all.add(t.getName(), t.getValue());
                }
            });
        }

//        all.forEach((t, u) -> {
//            log.info("t => " + t + " u => " + u);
//        });
        // 
        return all;
    }

    /**
     *
     * @param name Имя параметра
     * @return
     */
    @Override
    public List<String> getAttribute(String name) {
        log.debug("getAttribute => " + name);
        List<String> res = new LinkedList<>();
        
        if (name.contains("id_app_")) {
            Collection<UserAttribute> attrList = entity.getUserAttributeCollection();
            if (attrList != null) {
                attrList.forEach((t) -> {
                    if (t.isVisible_flag()) {
                        if (name.equals(t.getName())) {
                            res.add(t.getValue());
                        }
                    }
                });
            }
        } else {
            switch (name) {
                case "phone":
                    if (entity.getPhone() != null) {
                        res.add(entity.getPhone());
                    } else {
                        res.add("");
                    }
                    break;
                case "thirdName":
                    if (entity.getThirdName() != null) {
                        res.add(entity.getThirdName());
                    } else {
                        res.add("");
                    }
                    break;
                case "region":
                    if (entity.getUser_region() != null) {
                        res.add(entity.getUser_region().toString());
                    } else {
                        res.add("");
                    }
                    break;
                
                case "hash_type":
                    if (entity.getHash_type() != null) {
                        res.add(entity.getHash_type());
                    } else {
                        res.add("");
                    }
                    break;
                case "EMAIL_VERIFIED":
                    if (entity.isEmail_verified()) {
                        res.add("true");
                    } else {
                        res.add("false");
                    }
                    break;
                default:
                    return super.getAttribute(name);
            }
        }
        
        return res;
    }

    /**
     * Метод возвращает группы пользователя. Позволяет добавлять группы.
     *
     * @return возвращает списое групп пользователя
     */
//    @Override
//    public Set<GroupModel> getGroups() {
//        log.info("getGroups()");
//        return super.getGroups(); //To change body of generated methods, choose Tools | Templates.
//    }
//
    @Override
    public void removeRequiredAction(RequiredAction action) {
        log.debug("removeRequiredAction => " + action.name());
        UserRequiredAction temp = null;
        if (entity.getUserRequiredActionCollection() != null) {
            for (UserRequiredAction t : entity.getUserRequiredActionCollection()) {
                if (t.getTUserRequiredActionPK().getRequiredAction().equalsIgnoreCase(action.name())) {
                    temp = t;
                }
            }
        }
        
        if (temp != null) {
            entity.getUserRequiredActionCollection().remove(temp);
        } else {
            log.debug("ACTION NotFound => " + action);
        }
    }
    
    @Override
    public void addRequiredAction(RequiredAction action) {
        log.debug("addRequiredAction => " + action);
        //super.addRequiredAction(action); //To change body of generated methods, choose Tools | Templates.
        UserRequiredAction temp = null;
        if (entity.getUserRequiredActionCollection() != null) {
            for (UserRequiredAction t : entity.getUserRequiredActionCollection()) {
                if (t.getTUserRequiredActionPK().getRequiredAction().equalsIgnoreCase(action.name())) {
                    temp = t;
                }
            }
        }
        
        if (temp == null) {
            if (entity.getUserRequiredActionCollection() != null) {
                UserRequiredAction tempRA = new UserRequiredAction(action.name(), entity.getId());
                entity.getUserRequiredActionCollection().add(tempRA);
            } else {
                Collection<UserRequiredAction> tempList = new LinkedList<>();
                UserRequiredAction tempRA = new UserRequiredAction(action.name(), entity.getId());
                tempList.add(tempRA);
                entity.setUserRequiredActionCollection(tempList);
            }
        }
    }
    
    @Override
    public void removeRequiredAction(String action) {
        log.debug("removeRequiredAction => " + action);
        UserRequiredAction temp = null;
        if (entity.getUserRequiredActionCollection() != null) {
            for (UserRequiredAction t : entity.getUserRequiredActionCollection()) {
                if (t.getTUserRequiredActionPK().getRequiredAction().equalsIgnoreCase(action)) {
                    temp = t;
                }
            }
        }
        
        if (temp != null) {
            entity.getUserRequiredActionCollection().remove(temp);
        } else {
            log.debug("ACTION NotFound => " + action);
        }
    }
    
    @Override
    public void addRequiredAction(String action) {
        log.debug("addRequiredAction => " + action);
        //super.addRequiredAction(action); //To change body of generated methods, choose Tools | Templates.
        UserRequiredAction temp = null;
        if (entity.getUserRequiredActionCollection() != null) {
            for (UserRequiredAction t : entity.getUserRequiredActionCollection()) {
                if (t.getTUserRequiredActionPK().getRequiredAction().equalsIgnoreCase(action)) {
                    temp = t;
                }
            }
        }
        
        if (temp == null) {
            if (entity.getUserRequiredActionCollection() != null) {
                UserRequiredAction tempRA = new UserRequiredAction(action, entity.getId());
                entity.getUserRequiredActionCollection().add(tempRA);
            } else {
                Collection<UserRequiredAction> tempList = new LinkedList<>();
                UserRequiredAction tempRA = new UserRequiredAction(action, entity.getId());
                tempList.add(tempRA);
                entity.setUserRequiredActionCollection(tempList);
            }
        }
    }
    
    @Override
    public Set<String> getRequiredActions() {
        log.debug("getRequiredActions");
        Set<String> res = new HashSet();
        if (entity.getUserRequiredActionCollection() != null) {
            for (UserRequiredAction item : entity.getUserRequiredActionCollection()) {
                res.add(item.getTUserRequiredActionPK().getRequiredAction());
            }
            return res;
        }
        
        return super.getRequiredActions();
    }
    
    public Collection<UsersSmsMessages> getUserSmsMessages() {
        return entity.gettUsersSmsMessagesCollection();
    }
    
    public void addUserSmsMessages(UsersSmsMessages code) {
        log.info("addUserAuthSmsCode => " + code);
        if (entity.gettUsersSmsMessagesCollection() != null) {
            code.setUserId(entity);
            code.setDateCode(new Date());
            entity.gettUsersSmsMessagesCollection().add(code);
        } else {
            Collection<UsersSmsMessages> listCode = new LinkedList();
            code.setUserId(entity);
            listCode.add(code);
            code.setDateCode(new Date());
            entity.settUsersSmsMessagesCollection(listCode);
        }
    }

//    @Override
//    public UserFederatedStorageProvider getFederatedStorage() {
//        try {
//            log.debug("UserFederatedStorageProvider");
//            UserFederatedStorageProvider fed = super.getFederatedStorage();
//            log.debug("realm = " + realm + " EMAIL => " + EMAIL);
//            log.debug("fed => " + fed);
//            return super.getFederatedStorage(); //To change body of generated methods, choose Tools | Templates.
//        } catch (Exception e) {
//            log.log(Logger.Level.ERROR, e);
//        }
//        return null;
//    }
//
//    @Override
//    public int hashCode() {
//        log.debug("hashCode");
//        return super.hashCode(); //To change body of generated methods, choose Tools | Templates.
//    }
//
//    @Override
//    public boolean equals(Object o) {
//        log.debug("equals");
//        return super.equals(o); //To change body of generated methods, choose Tools | Templates.
//    }
//
//    @Override
//    public void setServiceAccountClientLink(String clientInternalId) {
//        log.debug("setServiceAccountClientLink");
//        super.setServiceAccountClientLink(clientInternalId); //To change body of generated methods, choose Tools | Templates.
//    }
//
//    @Override
//    public String getServiceAccountClientLink() {
//        log.debug("getServiceAccountClientLink");
//        return super.getServiceAccountClientLink(); //To change body of generated methods, choose Tools | Templates.
//    }
//
//    @Override
//    public void setFederationLink(String link) {
//        log.debug("setFederationLink => " + link);
//        super.setFederationLink(link); //To change body of generated methods, choose Tools | Templates.
//    }
//
//    @Override
//    public String getFederationLink() {
//        log.debug("getFederationLink");
//        return super.getFederationLink(); //To change body of generated methods, choose Tools | Templates.
//    }
//
//    @Override
//    public void deleteRoleMapping(RoleModel role) {
//        log.debug("deleteRoleMapping => " + role);
//        super.deleteRoleMapping(role); //To change body of generated methods, choose Tools | Templates.
//    }
//
//    @Override
//    protected Set<RoleModel> getFederatedRoleMappings() {
//        log.debug("getFederatedRoleMappings");
//        return super.getFederatedRoleMappings(); //To change body of generated methods, choose Tools | Templates.
//    }
//
//    @Override
//    public Set<RoleModel> getRoleMappings() {
//        log.debug("getRoleMappings");
//        return super.getRoleMappings(); //To change body of generated methods, choose Tools | Templates.
//    }
//
//    @Override
//    protected Set<RoleModel> getRoleMappingsInternal() {
//        log.debug("getRoleMappingsInternal");
//        return super.getRoleMappingsInternal(); //To change body of generated methods, choose Tools | Templates.
//    }
//
//    @Override
//    protected boolean appendDefaultRolesToRoleMappings() {
//        log.debug("appendDefaultRolesToRoleMappings");
//        return super.appendDefaultRolesToRoleMappings(); //To change body of generated methods, choose Tools | Templates.
//    }
//
//    @Override
//    public void grantRole(RoleModel role) {
//        log.debug("grantRole => " + role);
//        super.grantRole(role); //To change body of generated methods, choose Tools | Templates.
//    }
//
//    @Override
//    public boolean hasRole(RoleModel role) {
//        log.debug("hasRole => " + role);
//        return super.hasRole(role); //To change body of generated methods, choose Tools | Templates.
//    }
//
//    @Override
//    public Set<RoleModel> getClientRoleMappings(ClientModel app) {
//        log.debug("getClientRoleMappings => " + app);
//        return super.getClientRoleMappings(app); //To change body of generated methods, choose Tools | Templates.
//    }
//
//    @Override
//    public Set<RoleModel> getRealmRoleMappings() {
//        log.debug("getRealmRoleMappings");
//        return super.getRealmRoleMappings(); //To change body of generated methods, choose Tools | Templates.
//    }
//
//    @Override
//    public boolean isMemberOf(GroupModel group) {
//        log.debug("isMemberOf => " + group);
//        return super.isMemberOf(group); //To change body of generated methods, choose Tools | Templates.
//    }
//
//    @Override
//    public void leaveGroup(GroupModel group) {
//        log.debug("leaveGroup => " + group);
//        super.leaveGroup(group); //To change body of generated methods, choose Tools | Templates.
//    }
//
//    @Override
//    protected boolean appendDefaultGroups() {
//        log.debug("appendDefaultGroups");
//        return super.appendDefaultGroups(); //To change body of generated methods, choose Tools | Templates.
//    }
//
//    @Override
//    protected Set<GroupModel> getGroupsInternal() {
//        log.debug("getGroupsInternal");
//        return super.getGroupsInternal(); //To change body of generated methods, choose Tools | Templates.
//    }
//    //TODO: 12/01/2018 SECRET_QUESTION
//    public void setSecretQuestion(String question) {
//        entity.setSecret_question(question);
//    }
//        
//    public String getSecretQuestion() {
//        return entity.getSecret_question();
//    }
}
