/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package keycloak.bean;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author vasil
 */
@Entity
@Table(name = "t_users_sms")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "UsersSmsMessages.findAll", query = "SELECT t FROM UsersSmsMessages t")
    , @NamedQuery(name = "UsersSmsMessages.findById", query = "SELECT t FROM UsersSmsMessages t WHERE t.id = :id")
    , @NamedQuery(name = "UsersSmsMessages.findByDateCode", query = "SELECT t FROM UsersSmsMessages t WHERE t.dateCode = :dateCode")
    , @NamedQuery(name = "UsersSmsMessages.findByMessage", query = "SELECT t FROM UsersSmsMessages t WHERE t.message = :message")
    , @NamedQuery(name = "UsersSmsMessages.findByStatus", query = "SELECT t FROM UsersSmsMessages t WHERE t.status = :status")})
public class UsersSmsMessages implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Long id;
    @Basic(optional = false)
    @NotNull
    @Column(name = "date_code")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dateCode;
    @Column(name = "message")
    private String message;
    @Basic(optional = false)
    @NotNull
    @Column(name = "status")
    private boolean status;
    @Column(name = "check_code")
    private String check_code;
    @Column(name = "check_code_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date check_code_date;
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    @ManyToOne
    private UserEntity userId;

    public UsersSmsMessages() {
    }

    public UsersSmsMessages(Long id) {
        this.id = id;
    }

    public UsersSmsMessages(Long id, Date dateCode, boolean status) {
        this.id = id;
        this.dateCode = dateCode;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getDateCode() {
        return dateCode;
    }

    public void setDateCode(Date dateCode) {
        this.dateCode = dateCode;
    }

    public boolean getStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof UsersSmsMessages)) {
            return false;
        }
        UsersSmsMessages other = (UsersSmsMessages) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    public UserEntity getUserId() {
        return userId;
    }

    public void setUserId(UserEntity userId) {
        this.userId = userId;
    }

    public String getCheck_code() {
        return check_code;
    }

    public void setCheck_code(String check_code) {
        this.check_code = check_code;
    }

    public Date getCheck_code_date() {
        return check_code_date;
    }

    public void setCheck_code_date(Date check_code_date) {
        this.check_code_date = check_code_date;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "UsersSmsMessages{" + "id=" + id + ", message=" + message + ", status=" + status + ", check_code=" + check_code + ", check_code_date=" + check_code_date + '}';
    }

    
}
