package com.max.domain.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Set;

//он создается для ответа (т.е в регистр контроллере в стринге мы передаем что-то, и нам нужен ответ)
@JsonIgnoreProperties(ignoreUnknown = true) //игнорировать не знакомые свойтсва
public class CaptchaResponseDto {
    //переходим по ссылке после таблички будет ссылка на доп. информацию и там будет страница с json файлами

    //смотрим первую табличку и берем оттуда - success и error-code

    //оттуда берем
    private boolean success;

    @JsonAlias("error-codes") //используется для того, что в параметре json он прописан как "error-codes", а мы в переменной не ставили дифис - поэтому
    //если это не писать будут пробемы соответсвия
    private Set<String> errorCodes;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Set<String> getErrorCodes() {
        return errorCodes;
    }

    public void setErrorCodes(Set<String> errorCodes) {
        this.errorCodes = errorCodes;
    }
}
