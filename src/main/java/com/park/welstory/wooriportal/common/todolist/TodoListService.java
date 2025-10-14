package com.park.welstory.wooriportal.common.todolist;

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

    public Page<TodoListDTO> todoList (Long memberNum, Pageable pageable) {
        Page<TodoListEntity> entity = todoListRepository.findByMember_MemberNum(memberNum, pageable);
        Page<TodoListDTO> dto =  entity.map(entityTemp -> modelMapper.map(entityTemp, TodoListDTO.class));
        return dto;
    }

    public void addToDoList (TodoListDTO todoListDTO, Long memberNum){
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
        todoListRepository.save(entity);
    }

    @Transactional
    public void deleteToDoList(Long todoNum) {
        todoListRepository.deleteById(todoNum);
    }

    @Transactional
    public void toggleToDoStatus(Long todoNum) {
        try {
            TodoListEntity entity = todoListRepository.findById(todoNum)
                    .orElse(null);
            
            if (entity == null) {
                System.out.println("할일을 찾을 수 없습니다. ID: " + todoNum);
                return;
            }
            
            boolean currentStatus = entity.getTodoDone() != null ? entity.getTodoDone() : false;
            entity.setTodoDone(!currentStatus);
            
            if (!currentStatus) {
                entity.setDoneDate(LocalDateTime.now());
            } else {
                entity.setDoneDate(null);
            }
            todoListRepository.save(entity);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

}
