#include <AK/SinglyLinkedList.h>
#include <Kernel/Scheduler.h>
#include <Kernel/TimerQueue.h>


SinglyLinkedList<Timer>* g_timer_queue;

static u64 s_next_timer_due;
static u64 s_timer_id_count;
void TimerQueue::initialize()
{
 
    g_timer_queue = new SinglyLinkedList<Timer>();
}


u64 TimerQueue::add_timer(Timer& timer)
{
    ASSERT(timer.expires > Scheduler::g_uptime);
    timer.id = ++s_timer_id_count;
    g_timer_queue->sorted_insert_slow(timer);
    s_next_timer_due = g_timer_queue->first().expires;
    return s_timer_id_count;
}

u64 add_timer(u64 duration, TimeUnit unit, void (callback)())
{
	atuo timer = Timer();
    timer.expires = g_uptime + duration * unit;
    timer.callback = callback;
    return add_timer(timer);
}

void TimerQueue::sorted_insert_slow(T&& value)
{
	auto* new_node = new Node(move(value));
	new_node->value = value;

	if (!g_timer_queue->m_head || g_timer_queue->m_head->value > new_node->value) {
	    new_node->next = g_timer_queue->m_head;
	    if (!m_head)
	        m_tail = new_node;
	    m_head = new_node;
	    return;
	}

	Node* curr = m_head;
	while (curr->next && curr->next->value < new_node->value)
	{
	    curr = curr->next;
	}

	new_node->next = curr->next;
	curr->next = new_node;
	if(g_timer_queue->m_tail == curr)
	    g_timer_queue->m_tail = new_node;
}

void TimerQueue::fire_timer()
{
	ASSERT(s_next_timer_due == g_timer_queue->first().expires);
	while (! g_timer_queue->is_empty() && Scheduler::g_uptime > g_timer_queue->first().expires)
	{
	    auto timer = g_timer_queue->take_first();
	    timer.callback();
	}
	if (! g_timer_queue->is_empty())
	    s_next_timer_due = g_timer_queue->first().expires;
	else
	    s_next_timer_due = 0;
}