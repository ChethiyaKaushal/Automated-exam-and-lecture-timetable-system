package com.university.exam_lecture_timetable.dto.request;

public class UpdateUserStatusRequest {

    private Boolean enabled;
    private Boolean accountNonLocked;

    public UpdateUserStatusRequest() {
    }

    public UpdateUserStatusRequest(Boolean enabled, Boolean accountNonLocked) {
        this.enabled = enabled;
        this.accountNonLocked = accountNonLocked;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getAccountNonLocked() {
        return accountNonLocked;
    }

    public void setAccountNonLocked(Boolean accountNonLocked) {
        this.accountNonLocked = accountNonLocked;
    }
}
