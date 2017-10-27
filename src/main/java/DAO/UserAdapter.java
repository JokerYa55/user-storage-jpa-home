package DAO;

import java.util.Collection;
import java.util.Date;
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
import static keycloak.storage.util.hashUtil.encodeToHex;
import static keycloak.storage.util.hashUtil.genSalt;
//import static keycloak.storage.util.hashUtil.sha1ToString;
//import static keycloak.storage.util.hashUtil.encodeToHex;
import static keycloak.storage.util.hashUtil.sha1;
import org.keycloak.models.GroupModel;

// <!-- <property name="hibernate.show_sql" value="true"/> -->
/**
 * ����� ��� ������������� ������������ ������ Keycloak
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
        // ���������� ID
        keycloakId = StorageId.keycloakId(model, entity.getId().toString());
        this.em = em;
    }

    /**
     * ���������� ������ ��������� � �� � ���� password
     *
     * @return - ���������� ������ ����������� � ��
     */
    public String getPassword() {
        log.debug("getPassword => " + entity.getPassword());
        return entity.getPassword();
    }

    /**
     * ���������� hash ������ � ��������������� ������ � ��
     *
     * @param password - ������ ������������ � �������������� ����
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
     * �������� �������� ��� Salt �� ��
     *
     * @return
     */
    public String getSalt() {
        return entity.getSalt();
    }

    /**
     * ������������� �������� ��� Salt
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
     * ���������� ������ � ������� �� ���� username ��
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
     * ��������� ��������� �� ���������� Keycloak
     *
     * @param name ��� ���������
     * @param values �������� ��������� (������� �� ��������� ���� + �� �������
     * ���� � ������ ���������)
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
                // ��������� ������������� ���������                       
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

    /**
     * ����� ��������� ��������� ��������� �� ������� ���� � ��������� Keycloak
     *
     * @return
     */
    @Override
    public Map<String, List<String>> getAttributes() {
        log.debug("getAttributes");
        Map<String, List<String>> attrs = super.getAttributes();

        MultivaluedHashMap<String, String> all = new MultivaluedHashMap<>();
        all.putAll(attrs);
        // ��������� ���. ��������� � Keycloak
        log.info("************ Add user attibutes **************");

        if ((entity.getPhone() != null) && (entity.getPhone().length() > 0)) {
            // log.info("Add phone => " + entity.getPhone());
            all.add("phone", entity.getPhone());
        } else {
            all.add("phone", null);
        }

        if ((entity.getHash_type() != null) && (entity.getHash_type().length() > 0)) {
            //log.info("Add hash_type");
            all.add("hash_type", entity.getHash_type());
        } else {
            all.add("hash_type", null);
        }

        if ((entity.getThirdName() != null) && (entity.getThirdName().length() > 0)) {
            all.add("thirdName", entity.getThirdName());
        } else {
            all.add("thirdName", null);
        }

        if (entity.getUser_region() != null) {
            all.add("region", entity.getUser_region().toString());
        } else {
            all.add("region", null);
        }

        if (entity.getDescription() != null) {
            all.add("description", entity.getDescription());
        } else {
            all.add("description", null);
        }

        Collection<UserAttribute> attrList = entity.getUserAttributeCollection();
        if (attrList != null) {
            attrList.forEach((t) -> {
                if (t.isVisible_flag()) {
                    all.add(t.getName(), t.getValue());
                }
            });
        }

        // 
        return all;
    }

    /**
     *
     * @param name ��� ���������
     * @return
     */
    @Override
    public List<String> getAttribute(String name) {
        log.debug("getAttribute => " + name);
        List<String> res = new LinkedList<>();
        if (name.equals("phone")) {
            res.add(entity.getPhone());
        }
        if (name.equals("thirdName")) {
            res.add(entity.getThirdName());
        } else {
            return super.getAttribute(name);
        }
        return res;
    }

    /**
     * ����� ���������� ������ ������������. ��������� ��������� ������.
     *
     * @return ���������� ������ ����� ������������
     */
    @Override
    public Set<GroupModel> getGroups() {
        log.info("getGroups()");
        return super.getGroups(); //To change body of generated methods, choose Tools | Templates.
    }

}
