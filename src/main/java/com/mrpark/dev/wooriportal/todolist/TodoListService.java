package com.mrpark.dev.wooriportal.todolist;

import com.mrpark.dev.wooriportal.member.MemberRepository;
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

    /** 기존: void (/{group}/todolist POST에서 계속 사용) */
    public void addToDoList(TodoListDTO todoListDTO, Long memberNum) {
        TodoListEntity entity = modelMapper.map(todoListDTO, TodoListEntity.class);
        entity.setMember(memberRepository.findByMemberNum(memberNum));
        entity.setTodoDone(false);
        todoListRepository.save(entity);
    }

    /** 추가: 저장 후 DTO 반환 — /api/todolist 에서 todoNum 즉시 응답용 */
    public TodoListDTO addAndReturn(String title, Long memberNum) {
        TodoListEntity entity = new TodoListEntity();
        entity.setTodoTitle(title);
        entity.setTodoDone(false);
        entity.setMember(memberRepository.findByMemberNum(memberNum));
        TodoListEntity saved = todoListRepository.save(entity);
        return modelMapper.map(saved, TodoListDTO.class);
    }

    @Transactional
    public void updateToDoList(Long todoNum, TodoListDTO todoListDTO) {
        TodoListEntity entity = todoListRepository.findById(todoNum)
                .orElseThrow(() -> new RuntimeException("할일을 찾을 수 없습니다."));
        entity.setTodoTitle(todoListDTO.getTodoTitle());
        entity.setTodoContent(todoListDTO.getTodoContent());
    }

    @Transactional
    public void deleteToDoList(Long todoNum) {
        todoListRepository.deleteById(todoNum);
    }

    @Transactional
    public void toggleToDoStatus(Long todoNum) {
        TodoListEntity entity = todoListRepository.findById(todoNum)
                .orElseThrow(() -> new RuntimeException("할일을 찾을 수 없습니다."));
        boolean current = Boolean.TRUE.equals(entity.getTodoDone());
        entity.setTodoDone(!current);
        entity.setDoneDate(current ? null : LocalDateTime.now());
    }
}