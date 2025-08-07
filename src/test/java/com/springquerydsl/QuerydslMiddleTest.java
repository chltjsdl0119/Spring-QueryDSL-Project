package com.springquerydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.springquerydsl.entity.Member;
import com.springquerydsl.entity.Team;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.springquerydsl.entity.QMember.member;

@Transactional
public class QuerydslMiddleTest {

    @PersistenceContext
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before() {
        queryFactory = new JPAQueryFactory(em);

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);
        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    // 튜플 조회

    // 프로젝션 대상이 둘 이상일 때 사용
    //    List<Tuple> result = queryFactory
    //            .select(member.username, member.age)
    //            .from(member)
    //            .fetch();
    //    for (Tuple tuple : result) {
    //        String username = tuple.get(member.username);
    //        Integer age = tuple.get(member.age);
    //        System.out.println("username=" + username);
    //        System.out.println("age=" + age);
    //    }

    // 순수 JPA에서 DTO 조회 코드
    //    List<MemberDto> result = em.createQuery(
    //                    "select new study.querydsl.dto.MemberDto(m.username, m.age) " +
    //                            "from Member m", MemberDto.class)
    //            .getResultList();

    // 프로퍼티 접근 - Setter
    //    List<MemberDto> result = queryFactory
    //            .select(Projections.bean(MemberDto.class,
    //                    member.username,
    //                    member.age))
    //            .from(member)
    //            .fetch();

    // 필드 직접 접근
    //    List<MemberDto> result = queryFactory
    //            .select(Projections.fields(MemberDto.class,
    //                    member.username,
    //                    member.age))
    //            .from(member)
    //            .fetch();

    // 별칭이 다를 때
    //    List<UserDto> fetch = queryFactory
    //            .select(Projections.fields(UserDto.class,
    //                            member.username.as("name"),
    //                            ExpressionUtils.as(
    //                                    JPAExpressions
    //                                            .select(memberSub.age.max())
    //                                            .from(memberSub), "age")
    //                    )
    //            ).from(member)
    //            .fetch();

    // 생성자 사용
    //    List<MemberDto> result = queryFactory
    //            .select(Projections.constructor(MemberDto.class,
    //                    member.username,
    //                    member.age))
    //            .from(member)
    //            .fetch();

    // @QueryProjection 활용
    //    List<MemberDto> result = queryFactory
    //            .select(new QMemberDto(member.username, member.age))
    //            .from(member)
    //            .fetch();

    // distinct
    //    List<String> result = queryFactory
    //            .select(member.username).distinct()
    //            .from(member)
    //            .fetch();

    // 동적 쿼리 - BooleanBuilder 사용
    @Test
    public void 동적쿼리_BooleanBuilder() throws Exception {
        String usernameParam = "member1";
        Integer ageParam = 10;
        List<Member> result = searchMember1(usernameParam, ageParam);
        Assertions.assertThat(result.size()).isEqualTo(1);
    }
    private List<Member> searchMember1(String usernameCond, Integer ageCond) {
        BooleanBuilder builder = new BooleanBuilder();
        if (usernameCond != null) {
            builder.and(member.username.eq(usernameCond));
        }
        if (ageCond != null) {
            builder.and(member.age.eq(ageCond));
        }
        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }

    // 동적 쿼리 - Where 다중 파라미터 사용
    @Test
    public void 동적쿼리_WhereParam() throws Exception {
        String usernameParam = "member1";
        Integer ageParam = 10;
        List<Member> result = searchMember2(usernameParam, ageParam);
        Assertions.assertThat(result.size()).isEqualTo(1);
    }
    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory
                .selectFrom(member)
                .where(usernameEq(usernameCond), ageEq(ageCond))
                .fetch();
    }
    private BooleanExpression usernameEq(String usernameCond) {
        return usernameCond != null ? member.username.eq(usernameCond) : null;
    }
    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null;
    }

    // 조합 가능
    private BooleanExpression allEq(String usernameCond, Integer ageCond) {
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }

    // 쿼리 한번으로 대량 데이터 수정
    //    long count = queryFactory
    //            .update(member)
    //            .set(member.username, "비회원")
    //            .where(member.age.lt(28))
    //            .execute();
    //
    // 기존 숫자에 1 더하기
    //    long count = queryFactory
    //            .update(member)
    //            .set(member.age, member.age.add(1))
    //            .execute();
    //
    // 곱하기: multiply(x)
    //    update member
    //    set age = age + 1
    //
    // 쿼리 한번으로 대량 데이터 삭제
    //    long count = queryFactory
    //            .delete(member)
    //            .where(member.age.gt(18))
    //            .execute();

    // SQL function 호출하기
    // SQL function은 JPA와 같이 Dialect에 등록된 내용만 호출할 수 있다.
    // member M으로 변경하는 replace 함수 사용
    //    String result = queryFactory
    //            .select(Expressions.stringTemplate("function('replace', {0}, {1}, {2})",
    //                    member.username, "member", "M"))
    //            .from(member)
    //            .fetchFirst();
    //
    // 소문자로 변경해서 비교해라.
    //    .select(member.username)
    //    .from(member)
    //    .where(member.username.eq(Expressions.stringTemplate("function('lower', {0})", member.username)))
    //
    // lower 같은 ansi 표준 함수들은 querydsl이 상당부분 내장하고 있다. 따라서 다음과 같이 처리해도 결과는 같다.
    //    .where(member.username.eq(member.username.lower()))
}
