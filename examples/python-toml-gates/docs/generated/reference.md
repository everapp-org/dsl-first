<!-- GENERATED from model/ by tools/generate_docs.py. Do not edit. -->

# task-app — reference

A minimal task application.

## Context `tasks`

Create a task, complete it, and list the tasks.

### command `tasks.archive`

Move a done task out of the list.

- status: **not-built**
- reason: No decision yet on how long an archived task is kept.
- receives: `task_id`: int

### command `tasks.complete`

Mark one task as done.

- status: **ready**
- receives: `task_id`: int
- returns: `task_id`: int, `done`: bool
- emits: `task.completed`

### command `tasks.create`

Create a task with a title.

- status: **ready**
- receives: `title`: str
- returns: `task_id`: int
- emits: `task.created`

### query `tasks.list`

List every task.

- status: **ready**
- receives: nothing
- returns a list, each row: `task_id`: int, `title`: str, `done`: bool

### event `task.completed`

A task is done.

- carries: `task_id`: int

### event `task.created`

A task now exists.

- carries: `task_id`: int, `title`: str

## Context `audit`

Keep a record of every command the application receives.

### query `audit.entries`

List the recorded commands, oldest first.

- status: **ready**
- receives: nothing
- returns a list, each row: `operation`: str

## What is declared and not built

- `tasks.archive` — No decision yet on how long an archived task is kept.

## Wirings

| wiring | from | field | to | payload field |
|---|---|---|---|---|
| complete-from-list | `tasks.list` | `task_id` | `tasks.complete` | `task_id` |
