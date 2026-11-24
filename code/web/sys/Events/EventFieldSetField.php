<?php /** @noinspection PhpMissingFieldTypeInspection */

class EventFieldSetField extends DataObject {
	public $__table = 'event_field_set_field';
	public $id;
	public $eventFieldId;
	public $eventFieldSetId;
	public $weight;

	static function getObjectStructure(string $context = ''): array {
		require_once ROOT_DIR . '/sys/Events/EventField.php';
		return [
			'id' => [
				'property' => 'id',
				'type' => 'label',
				'label' => 'Id',
				'description' => 'The unique id',
			],
			'eventFieldSetId' => [
				'property' => 'eventFieldSetId',
				'type' => 'foreignKey',
				'label' => 'Event Field Set',
				'description' => 'The field set this belongs to',
			],
			'eventFieldId' => [
				'property' => 'eventFieldId',
				'type' => 'enum',
				'values' => EventField::getEventFieldList(),
				'label' => 'Field',
				'description' => 'The field to include',
				'required' => true,
			],
			'weight' => [
				'property' => 'weight',
				'type' => 'numeric',
				'label' => 'Weight',
				'description' => 'Controls sort order. Lower weights display first.',
				'default' => 0,
				'required' => true,
			],
		];
	}
}