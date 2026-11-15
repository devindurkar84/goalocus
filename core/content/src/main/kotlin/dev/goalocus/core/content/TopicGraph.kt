package dev.goalocus.core.content

import dev.goalocus.core.domain.model.Topic
import dev.goalocus.core.domain.model.TopicId
import java.util.PriorityQueue

/**
 * The hard-prerequisite graph of all topics, built once and immutable.
 *
 * Construction validates the graph (unique ids, known prerequisites, no cycles) and caches a
 * deterministic topological order, so queries are simple linear scans with no recursion.
 */
class TopicGraph(topics: Collection<Topic>) {
    private val byId: Map<TopicId, Topic> = topics.associateBy(Topic::id)
    private val dependents: Map<TopicId, List<TopicId>>
    private val position: Map<TopicId, Int>

    /** Every topic id, prerequisites before dependents; ties broken alphabetically. */
    val topologicalOrder: List<TopicId>

    init {
        require(byId.size == topics.size) { "Topic ids must be unique" }
        for (topic in topics) {
            for (prereq in topic.hardPrereqs) {
                require(prereq in byId) { "${topic.id} needs unknown hard prerequisite $prereq" }
                require(prereq != topic.id) { "${topic.id} lists itself as a prerequisite" }
            }
        }
        dependents = buildMap<TopicId, MutableList<TopicId>> {
            for (topic in topics) {
                for (prereq in topic.hardPrereqs.distinct()) getOrPut(prereq) { mutableListOf() }.add(topic.id)
            }
        }
        topologicalOrder = kahn(topics)
        position = topologicalOrder.withIndex().associate { (index, id) -> id to index }
    }

    val size: Int get() = byId.size

    operator fun get(id: TopicId): Topic? = byId[id]

    fun prerequisitesOf(id: TopicId): List<TopicId> = byId[id]?.hardPrereqs.orEmpty()

    fun dependentsOf(id: TopicId): List<TopicId> = dependents[id].orEmpty()

    /** Topics not yet learned whose hard prerequisites are all learned, in topological order. O(V + E). */
    fun frontier(learned: Set<TopicId>): List<Topic> = topologicalOrder.mapNotNull { id ->
        byId.getValue(id).takeIf { id !in learned && it.hardPrereqs.all(learned::contains) }
    }

    /**
     * Every unlearned topic that has to be learned before [target] can start, prerequisites first.
     * Used to pull prerequisites forward for an unlock gate instead of making the learner wait.
     */
    fun missingPrerequisites(target: TopicId, learned: Set<TopicId>): List<TopicId> {
        require(target in byId) { "Unknown topic $target" }
        val missing = HashSet<TopicId>()
        val stack = ArrayDeque(prerequisitesOf(target))
        while (stack.isNotEmpty()) {
            val id = stack.removeLast()
            if (id in learned || !missing.add(id)) continue
            stack.addAll(prerequisitesOf(id))
        }
        return missing.sortedBy(position::getValue)
    }

    private fun kahn(topics: Collection<Topic>): List<TopicId> {
        val inDegree = HashMap<TopicId, Int>(topics.size)
        for (topic in topics) inDegree[topic.id] = topic.hardPrereqs.distinct().size
        val ready = PriorityQueue<TopicId>(compareBy(TopicId::value))
        inDegree.filterValues { it == 0 }.keys.forEach(ready::add)
        val order = ArrayList<TopicId>(topics.size)
        while (ready.isNotEmpty()) {
            val id = ready.poll()
            order += id
            for (dependent in dependentsOf(id)) {
                val remaining = inDegree.getValue(dependent) - 1
                inDegree[dependent] = remaining
                if (remaining == 0) ready.add(dependent)
            }
        }
        require(order.size == topics.size) {
            val stuck = inDegree.filterValues { it > 0 }.keys.map(TopicId::value).sorted()
            "Hard prerequisites contain a cycle involving: ${stuck.joinToString()}"
        }
        return order
    }
}
