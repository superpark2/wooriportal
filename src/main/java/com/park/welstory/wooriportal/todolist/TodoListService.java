package com.park.welstory.wooriportal.todolist;

import com.park.welstory.wooriportal.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TodoListService {

    private final ModelMapper modelMapper = new ModelMapper();
    private final TodoListRepository todoListRepository;
    private final MemberRepository memberRepository;

    public Page<TodoListDTO> todoList(Long memberNum, Pageable pageable) {
        return todoListRepository.findByMember_MemberNum(memberNum, pageable)
                .map(entity -> modelMapper.map(entity, TodoListDTO.class));
    }

    public void addToDoList(TodoListDTO todoListDTO, Long memberNum) {
        TodoListEntity entity = modelMapper.map(todoListDTO, TodoListEntity.class);
        entity.setMember(memberRepository.findByMemberNum(memberNum));
        entity.setTodoDone(false);
        todoListRepository.save(entity);
    }

    @Transactional
    public void updateToDoList(Long todoNum, TodoListDTO todoListDTO) {
        TodoListEntity entity = todoListRepository.findById(todoNum)
                .orElseThrow(() -> new RuntimeException("할일을 찾을 수 없습니다."));
        entity.setTodoTitle(todoListDTO.getTodoTitle());
        entity.setTodoContent(todoListDTO.getTodoContent());
        // save() 불필요 — @Transactional 내 dirty checking으로 자동 반영
    }

    @Transactional
    public void deleteToDoList(Long todoNum) {
        todoListRepository.deleteById(todoNum);
    }

    @Transactional
    public void toggleToDoStatus(Long todoNum) {
        // 버그수정: try-catch 제거 (예외는 호출자에게 전파, e.printStackTrace 불필요)
        // 버그수정: orElse(null) + null체크 → orElseThrow로 명확하게
        TodoListEntity entity = todoListRepository.findById(todoNum)
                .orElseThrow(() -> new RuntimeException("할일을 찾을 수 없습니다."));

        boolean currentStatus = Boolean.TRUE.equals(entity.getTodoDone());
        entity.setTodoDone(!currentStatus);
        entity.setDoneDate(currentStatus ? null : LocalDateTime.now());
        // save() 불필요 — @Transactional 내 dirty checking으로 자동 반영
    }
}
