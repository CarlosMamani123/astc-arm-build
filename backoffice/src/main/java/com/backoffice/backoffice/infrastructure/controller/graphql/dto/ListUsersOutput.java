package com.backoffice.backoffice.infrastructure.controller.graphql.dto;

import com.backoffice.backoffice.domain.model.User;

import java.util.List;
import org.eclipse.microprofile.graphql.Type;

@Type
public class ListUsersOutput {

    private List<UserOutput> data;
    private PageInfoOutput pageInfo;

    public static ListUsersOutput fromDomain(List<User> users) {

        ListUsersOutput output = new ListUsersOutput();

        output.setData(
                users.stream()
                        .map(UserOutput::fromDomain)
                        .toList()
        );

        return output;
    }

    public List<UserOutput> getData() {
        return data;
    }

    public void setData(List<UserOutput> data) {
        this.data = data;
    }

    public PageInfoOutput getPageInfo() {
        return pageInfo;
    }

    public void setPageInfo(PageInfoOutput pageInfo) {
        this.pageInfo = pageInfo;
    }
}